import express from 'express';
import os from 'os';
import QRCode from 'qrcode';
import { bilibiliService } from '../services/bilibili.js';
import { recommendationService } from '../services/recommender.js';

const router = express.Router();

// Helper to get local IPv4 addresses (LAN IPs)
function getLanIps() {
  const interfaces = os.networkInterfaces();
  const addresses = [];
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        addresses.push({ name, address: iface.address });
      }
    }
  }
  return addresses;
}

// Runtime in-memory preferences & creators store
let runtimeConfig = {
  preferences: (process.env.DEFAULT_PREFERENCES || '科技前沿, 数码测评, 编程开发, 硬件极客, 游戏解说, 硬核科普')
    .split(/[,，]+/)
    .map(s => s.trim())
    .filter(Boolean),
  creators: (process.env.DEFAULT_FOLLOWED_CREATORS || '影视飓风, 极客湾Geekerwan, 老师好我叫何同学, 稚晖君, 罗翔说刑法, 泛式')
    .split(/[,，]+/)
    .map(s => s.trim())
    .filter(Boolean),
  expandRelated: process.env.EXPAND_RELATED_VIDEOS !== 'false',
  pageSize: Number(process.env.RECOMMEND_PAGE_SIZE) || 10,
};

/**
 * 1. Health check & status
 */
router.get('/health', (req, res) => {
  const lanIps = getLanIps();
  res.json({
    code: 0,
    status: 'ok',
    message: 'Bilibili AI Recommendation Service is healthy',
    data: {
      time: new Date().toISOString(),
      lanIps,
      llm: recommendationService.getConfig(),
      preferences: runtimeConfig.preferences,
      creators: runtimeConfig.creators,
    },
  });
});

/**
 * 2. Get LAN IPs (for mobile connection QR code)
 */
router.get('/lan-ips', (req, res) => {
  res.json({
    code: 0,
    data: getLanIps(),
  });
});

/**
 * 2.5 Generate QR Code SVG dynamically
 */
router.get('/qrcode', async (req, res) => {
  const text = req.query.text || '';
  if (!text) return res.status(400).send('Missing text parameter');
  try {
    const svg = await QRCode.toString(text, {
      type: 'svg',
      margin: 1,
      color: {
        dark: '#212121',
        light: '#FFFFFF',
      },
    });
    res.setHeader('Content-Type', 'image/svg+xml');
    res.setHeader('Cache-Control', 'public, max-age=86400');
    res.send(svg);
  } catch (e) {
    res.status(500).send(e.message);
  }
});

/**
 * 2.6 Image Proxy to bypass anti-hotlink restrictions
 */
router.get('/proxy-image', async (req, res) => {
  const imageUrl = req.query.url;
  if (!imageUrl) return res.status(400).send('Missing url parameter');

  try {
    let cleanUrl = String(imageUrl).trim();
    if (cleanUrl.startsWith('//')) cleanUrl = 'https:' + cleanUrl;
    if (cleanUrl.startsWith('http://')) cleanUrl = cleanUrl.replace('http://', 'https://');

    const response = await fetch(cleanUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://www.bilibili.com/',
      },
    });

    if (!response.ok) {
      return res.status(response.status).send(`Upstream returned ${response.status}`);
    }

    const contentType = response.headers.get('content-type') || 'image/jpeg';
    res.setHeader('Content-Type', contentType);
    res.setHeader('Cache-Control', 'public, max-age=86400');

    const arrayBuffer = await response.arrayBuffer();
    res.send(Buffer.from(arrayBuffer));
  } catch (e) {
    res.status(500).send(e.message);
  }
});

/**
 * 3. Get / Update Config
 */
router.get('/config', (req, res) => {
  res.json({
    code: 0,
    data: {
      llm: recommendationService.getConfig(),
      ...runtimeConfig,
    },
  });
});

router.post('/config', (req, res) => {
  const { apiKey, baseURL, model, temperature, preferences, creators, expandRelated, pageSize } = req.body || {};

  recommendationService.updateConfig({ apiKey, baseURL, model, temperature });

  if (Array.isArray(preferences)) {
    runtimeConfig.preferences = preferences.map(p => String(p).trim()).filter(Boolean);
  } else if (typeof preferences === 'string') {
    runtimeConfig.preferences = preferences.split(/[,，]+/).map(p => p.trim()).filter(Boolean);
  }

  if (Array.isArray(creators)) {
    runtimeConfig.creators = creators.map(c => String(c).trim()).filter(Boolean);
  } else if (typeof creators === 'string') {
    runtimeConfig.creators = creators.split(/[,，]+/).map(c => c.trim()).filter(Boolean);
  }

  if (expandRelated !== undefined) {
    runtimeConfig.expandRelated = Boolean(expandRelated);
  }

  if (pageSize !== undefined) {
    runtimeConfig.pageSize = Math.min(30, Math.max(1, Number(pageSize) || 10));
  }

  res.json({
    code: 0,
    message: '配置已更新成功',
    data: {
      llm: recommendationService.getConfig(),
      ...runtimeConfig,
    },
  });
});

/**
 * 4. Inspect Raw Candidates (unlogged-in)
 */
router.get('/candidates', async (req, res) => {
  try {
    const candidates = await bilibiliService.buildCandidatePool({
      creators: runtimeConfig.creators,
      preferences: runtimeConfig.preferences,
      expandRelated: runtimeConfig.expandRelated,
      includePopular: true,
      includeRanking: true,
    });
    res.json({
      code: 0,
      total: candidates.length,
      data: candidates,
    });
  } catch (err) {
    res.status(500).json({ code: 500, message: err.message });
  }
});

/**
 * 5. Main Recommendation Endpoint (compatible with Android app & Web UI)
 * Supports GET and POST
 */
const handleRecommend = async (req, res) => {
  const query = req.method === 'GET' ? req.query : req.body;

  const pageSize = Number(query.pageSize || query.ps || runtimeConfig.pageSize || 10);
  const prompt = query.prompt || '';
  const creators = query.creators
    ? (Array.isArray(query.creators) ? query.creators : query.creators.split(/[,，]+/))
    : runtimeConfig.creators;
  const preferences = query.preferences
    ? (Array.isArray(query.preferences) ? query.preferences : query.preferences.split(/[,，]+/))
    : runtimeConfig.preferences;
  const expandRelated = query.expandRelated !== undefined ? (query.expandRelated === true || query.expandRelated === 'true') : runtimeConfig.expandRelated;

  try {
    // 1. Fetch unlogged-in candidates
    const candidates = await bilibiliService.buildCandidatePool({
      creators,
      preferences,
      expandRelated,
      includePopular: true,
      includeRanking: true,
    });

    // 2. Rank candidates through AI Chat Completions / Smart Heuristic
    const recommended = await recommendationService.rankAndRecommend({
      candidates,
      userPreferences: preferences,
      followedCreators: creators,
      customPrompt: prompt,
      limit: pageSize,
    });

    // Format response so it satisfies Android Retrofit models:
    // data.item: List<VideoItem> (like /x/web-interface/index/top/feed/rcmd)
    // data.list: List<VideoItem> (like /x/web-interface/popular)
    res.json({
      code: 0,
      message: '0',
      data: {
        item: recommended,
        list: recommended,
        total: recommended.length,
        candidate_pool_size: candidates.length,
        llm_model: recommendationService.model,
      },
    });
  } catch (err) {
    console.error('[API /recommend] Error:', err);
    res.status(500).json({
      code: -1,
      message: `推荐生成失败: ${err.message}`,
      data: { item: [], list: [] },
    });
  }
};

router.get('/recommend', handleRecommend);
router.post('/recommend', handleRecommend);

export default router;
