/**
 * Bilibili Unlogged-in Video Scraper & Normalization Service
 * Fetches videos from public Bilibili APIs without requiring user credentials.
 */

class BilibiliService {
  constructor() {
    this.guestCookie = '';
    this.cookieExpireTime = 0;
    this.defaultUserAgent =
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
  }

  /**
   * Automatically refresh anonymous guest cookies (buvid3 / b_nut)
   */
  async ensureGuestCookie() {
    const now = Date.now();
    if (this.guestCookie && now < this.cookieExpireTime) {
      return this.guestCookie;
    }

    try {
      const res = await fetch('https://www.bilibili.com/', {
        headers: {
          'User-Agent': this.defaultUserAgent,
        },
      });
      const rawCookies = res.headers.getSetCookie();
      if (rawCookies && rawCookies.length > 0) {
        this.guestCookie = rawCookies.map(c => c.split(';')[0]).join('; ');
      } else {
        // Fallback default guest buvid
        this.guestCookie = 'buvid3=34B338FF-7621-EC25-5241-F03D4DCDEB9F33623infoc; b_nut=' + Math.floor(now / 1000);
      }
      this.cookieExpireTime = now + 1000 * 60 * 60; // 1 hour
    } catch (e) {
      console.warn('[BilibiliService] Failed to fetch guest cookie, using fallback:', e.message);
      this.guestCookie = 'buvid3=34B338FF-7621-EC25-5241-F03D4DCDEB9F33623infoc';
    }
    return this.guestCookie;
  }

  getHeaders(referer = 'https://www.bilibili.com/') {
    return {
      'User-Agent': this.defaultUserAgent,
      'Referer': referer,
      'Cookie': this.guestCookie,
    };
  }

  /**
   * Helper: Normalize image URL with https:
   */
  normalizePic(url) {
    if (!url) return '';
    if (url.startsWith('//')) return 'https:' + url;
    if (url.startsWith('http://')) return url.replace('http://', 'https://');
    return url;
  }

  /**
   * Helper: Parse duration string (e.g. "04:32" or "1:02:15") into seconds
   */
  parseDuration(duration) {
    if (typeof duration === 'number') return duration;
    if (!duration || typeof duration !== 'string') return 0;
    const parts = duration.split(':').map(Number);
    if (parts.length === 2) {
      return (parts[0] || 0) * 60 + (parts[1] || 0);
    }
    if (parts.length === 3) {
      return (parts[0] || 0) * 3600 + (parts[1] || 0) * 60 + (parts[2] || 0);
    }
    return parseInt(duration, 10) || 0;
  }

  /**
   * Standardize video object into Android VideoItem schema
   */
  normalizeVideo(v, source = '推荐') {
    if (!v) return null;

    const bvid = v.bvid || (v.param && v.param.startsWith('BV') ? v.param : '') || '';
    if (!bvid) return null;

    const rawTitle = v.title || '';
    // Strip HTML highlighting tags like <em class="keyword">...</em>
    const cleanTitle = rawTitle.replace(/<[^>]+>/g, '').trim();

    const pic = this.normalizePic(v.pic || v.cover || '');
    const duration = this.parseDuration(v.duration || v.length || 0);
    const pubdate = v.pubdate || v.pub_time || v.senddate || Math.floor(Date.now() / 1000);

    // Extract Owner
    let ownerName = '';
    let ownerMid = 0;
    let ownerFace = '';
    if (v.owner) {
      ownerName = v.owner.name || '';
      ownerMid = v.owner.mid || 0;
      ownerFace = this.normalizePic(v.owner.face);
    } else {
      ownerName = v.author || v.up_name || 'B站创作者';
      ownerMid = v.mid || 0;
      ownerFace = this.normalizePic(v.up_face);
    }

    // Extract Stat
    const stat = {
      aid: v.aid || v.id || 0,
      view: v.stat?.view ?? v.play ?? 0,
      danmaku: v.stat?.danmaku ?? v.video_review ?? 0,
      reply: v.stat?.reply ?? 0,
      favorite: v.stat?.favorite ?? v.favorites ?? 0,
      coin: v.stat?.coin ?? v.coins ?? 0,
      share: v.stat?.share ?? 0,
      like: v.stat?.like ?? v.like ?? 0,
    };

    // Extract rcmd reason
    let rcmdReason = null;
    if (v.rcmd_reason && typeof v.rcmd_reason === 'object' && v.rcmd_reason.content) {
      rcmdReason = { content: v.rcmd_reason.content };
    } else if (typeof v.rcmd_reason === 'string' && v.rcmd_reason.trim()) {
      rcmdReason = { content: v.rcmd_reason.trim() };
    }

    return {
      aid: Number(v.aid || v.id || 0),
      bvid,
      cid: Number(v.cid || 0),
      title: cleanTitle,
      pic,
      desc: (v.desc || v.description || '').slice(0, 300),
      duration,
      pubdate,
      owner: {
        mid: Number(ownerMid),
        name: ownerName,
        face: ownerFace,
      },
      stat,
      rcmd_reason: rcmdReason,
      achievement: v.achievement || null,
      rank: v.rank || null,
      sourceTag: source,
    };
  }

  /**
   * 1. 综合热门 (Popular Videos)
   */
  async fetchPopular(page = 1, pageSize = 20) {
    await this.ensureGuestCookie();
    try {
      const url = `https://api.bilibili.com/x/web-interface/popular?pn=${page}&ps=${pageSize}`;
      const res = await fetch(url, { headers: this.getHeaders() });
      const data = await res.json();
      if (data.code === 0 && data.data?.list) {
        return data.data.list
          .map(v => this.normalizeVideo(v, '综合热门'))
          .filter(Boolean);
      }
    } catch (e) {
      console.error('[fetchPopular] Error:', e.message);
    }
    return [];
  }

  /**
   * 2. 全站与分区排行榜 (Ranking Videos)
   * rid: 0 全站, 1 动画, 4 游戏, 36 知识, 188 数码科技, 160 生活
   */
  async fetchRanking(rid = 0) {
    await this.ensureGuestCookie();
    try {
      const url = `https://api.bilibili.com/x/web-interface/ranking/v2?rid=${rid}&type=all`;
      const res = await fetch(url, { headers: this.getHeaders() });
      const data = await res.json();
      if (data.code === 0 && data.data?.list) {
        return data.data.list
          .map((v, idx) => {
            const item = this.normalizeVideo(v, rid === 0 ? '全站排行' : `分区排行(#${rid})`);
            if (item) item.rank = idx + 1;
            return item;
          })
          .filter(Boolean);
      }
    } catch (e) {
      console.error('[fetchRanking] Error:', e.message);
    }
    return [];
  }

  /**
   * 3. 首页推荐流 (Index Rcmd Feed)
   */
  async fetchIndexFeed(freshIdx = 1, pageSize = 10) {
    await this.ensureGuestCookie();
    try {
      const url = `https://api.bilibili.com/x/web-interface/index/top/feed/rcmd?ps=${pageSize}&fresh_idx=${freshIdx}`;
      const res = await fetch(url, { headers: this.getHeaders() });
      const data = await res.json();
      if (data.code === 0 && data.data?.item) {
        return data.data.item
          .map(v => this.normalizeVideo(v, '首页推荐'))
          .filter(Boolean);
      }
    } catch (e) {
      console.error('[fetchIndexFeed] Error:', e.message);
    }
    return [];
  }

  /**
   * 4. 视频相关推荐 (Archive Related Videos)
   * Fetches up to 40 related videos for a given BVID without login
   */
  async fetchRelated(bvid, sourceTag = '相关推荐') {
    if (!bvid) return [];
    await this.ensureGuestCookie();
    try {
      const url = `https://api.bilibili.com/x/web-interface/archive/related?bvid=${bvid}`;
      const res = await fetch(url, { headers: this.getHeaders(`https://www.bilibili.com/video/${bvid}`) });
      const data = await res.json();
      if (data.code === 0 && Array.isArray(data.data)) {
        return data.data
          .map(v => this.normalizeVideo(v, sourceTag))
          .filter(Boolean);
      }
    } catch (e) {
      console.error(`[fetchRelated] Error for ${bvid}:`, e.message);
    }
    return [];
  }

  /**
   * 5. 关键词搜索视频 (Video Search with guest cookie)
   * order: pubdate (最新), totalrank (综合), click (播放)
   */
  async searchVideos(keyword, order = 'pubdate', pageSize = 20) {
    if (!keyword) return [];
    await this.ensureGuestCookie();
    try {
      const url = `https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword=${encodeURIComponent(
        keyword
      )}&order=${order}&page_size=${pageSize}`;
      const res = await fetch(url, {
        headers: this.getHeaders('https://search.bilibili.com/'),
      });
      const data = await res.json();
      if (data.code === 0 && data.data?.result) {
        return data.data.result
          .map(v => this.normalizeVideo(v, `搜索:${keyword}`))
          .filter(Boolean);
      }
    } catch (e) {
      console.error(`[searchVideos] Error for ${keyword}:`, e.message);
    }
    return [];
  }

  /**
   * 6. 从关注UP主抓取最新视频
   */
  async fetchCreatorVideos(creatorName, maxCount = 2) {
    if (!creatorName) return [];
    try {
      const searchResults = await this.searchVideos(creatorName, 'pubdate', 10);
      // Filter videos where author matches creatorName
      const exactMatches = searchResults.filter(
        v => v.owner?.name?.toLowerCase().includes(creatorName.toLowerCase()) ||
             creatorName.toLowerCase().includes(v.owner?.name?.toLowerCase())
      );
      const list = exactMatches.length > 0 ? exactMatches : searchResults.slice(0, maxCount);
      return list.slice(0, maxCount).map(v => {
        v.sourceTag = `关注UP[${creatorName}]直发`;
        return v;
      });
    } catch (e) {
      console.error(`[fetchCreatorVideos] Error for ${creatorName}:`, e.message);
    }
    return [];
  }

  /**
   * 7. 综合候选池构建器 (Candidate Pool Builder)
   * Combines:
   * 1. Followed creators' recent videos
   * 2. Related videos expanded from those creators' videos (archive/related)
   * 3. Topic preference keyword searches
   * 4. Fresh Popular & Ranking videos
   */
  async buildCandidatePool({
    creators = [],
    preferences = [],
    includePopular = true,
    includeRanking = true,
    expandRelated = true,
  } = {}) {
    console.log('[CandidatePool] Building candidate pool with creators:', creators.length, 'prefs:', preferences.length);
    const candidateMap = new Map();

    const addVideos = (list) => {
      for (const v of list) {
        if (v && v.bvid && !candidateMap.has(v.bvid)) {
          candidateMap.set(v.bvid, v);
        }
      }
    };

    // Step 1: Followed creators' recent videos
    const creatorVideos = [];
    for (const name of creators.slice(0, 6)) {
      const list = await this.fetchCreatorVideos(name.trim(), 2);
      creatorVideos.push(...list);
    }
    addVideos(creatorVideos);
    console.log(`[CandidatePool] Fetched ${creatorVideos.length} direct videos from creators.`);

    // Step 2: Expand related videos from creators' recent videos
    if (expandRelated && creatorVideos.length > 0) {
      // Pick top 3 creator videos to expand related
      const toExpand = creatorVideos.slice(0, 3);
      for (const cv of toExpand) {
        const relatedList = await this.fetchRelated(cv.bvid, `关注UP[${cv.owner?.name || '关注'}]相关拓展`);
        addVideos(relatedList.slice(0, 8));
      }
      console.log(`[CandidatePool] After related expansion, total candidates: ${candidateMap.size}`);
    }

    // Step 3: Preference topic searches (e.g. 科技前沿, 游戏, etc.)
    for (const pref of preferences.slice(0, 4)) {
      const trimmed = pref.trim();
      if (trimmed) {
        const searchList = await this.searchVideos(trimmed, 'pubdate', 6);
        addVideos(searchList);
      }
    }

    // Step 4: Popular and Ranking
    if (includePopular) {
      const popList = await this.fetchPopular(1, 15);
      addVideos(popList);
    }

    if (includeRanking) {
      const rankList = await this.fetchRanking(0);
      addVideos(rankList.slice(0, 15));
    }

    const allCandidates = Array.from(candidateMap.values());
    console.log(`[CandidatePool] Final unique candidate pool size: ${allCandidates.length}`);
    return allCandidates;
  }
}

export const bilibiliService = new BilibiliService();
