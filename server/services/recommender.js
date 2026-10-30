/**
 * AI Recommendation Engine
 * Integrates OpenAI-compatible Chat Completions API with fallback semantic scoring.
 */

class RecommendationService {
  constructor() {
    this.apiKey = process.env.OPENAI_API_KEY || '';
    this.baseURL = process.env.OPENAI_BASE_URL || 'https://api.openai.com/v1';
    this.model = process.env.OPENAI_MODEL || 'gpt-4o-mini';
    this.temperature = 0.7;
  }

  updateConfig({ apiKey, baseURL, model, temperature }) {
    if (apiKey !== undefined) this.apiKey = apiKey.trim();
    if (baseURL !== undefined) {
      let b = baseURL.trim();
      if (b.endsWith('/')) b = b.slice(0, -1);
      this.baseURL = b;
    }
    if (model !== undefined) this.model = model.trim();
    if (temperature !== undefined) this.temperature = Number(temperature) || 0.7;
    console.log('[Recommender] Config updated. Model:', this.model, 'BaseURL:', this.baseURL, 'HasKey:', Boolean(this.apiKey));
  }

  getConfig() {
    return {
      hasApiKey: Boolean(this.apiKey),
      apiKeyMasked: this.apiKey ? `${this.apiKey.slice(0, 4)}...${this.apiKey.slice(-4)}` : '',
      baseURL: this.baseURL,
      model: this.model,
      temperature: this.temperature,
    };
  }

  /**
   * Main recommendation method
   * Takes candidate pool + user preferences and returns ranked recommendations
   */
  async rankAndRecommend({
    candidates = [],
    userPreferences = [],
    followedCreators = [],
    customPrompt = '',
    limit = 10,
  }) {
    if (!candidates || candidates.length === 0) {
      return [];
    }

    console.log(`[Recommender] Starting ranking for ${candidates.length} candidates, requested limit: ${limit}`);

    // If API key is available, call Chat Completions API
    if (this.apiKey) {
      try {
        const aiResults = await this.callChatCompletions({
          candidates,
          userPreferences,
          followedCreators,
          customPrompt,
          limit,
        });

        if (aiResults && aiResults.length > 0) {
          console.log(`[Recommender] Successfully received ${aiResults.length} recommendations from LLM.`);
          return this.mergeRecommendations(candidates, aiResults, limit);
        }
      } catch (err) {
        console.warn('[Recommender] Chat completions call failed, falling back to smart heuristic:', err.message);
      }
    } else {
      console.log('[Recommender] No OPENAI_API_KEY provided. Using smart heuristic scoring.');
    }

    // Fallback: Smart Heuristic Recommender
    return this.fallbackHeuristicRecommend({
      candidates,
      userPreferences,
      followedCreators,
      customPrompt,
      limit,
    });
  }

  /**
   * Call OpenAI-compatible Chat Completions
   */
  async callChatCompletions({ candidates, userPreferences, followedCreators, customPrompt, limit }) {
    // Trim candidate list to top 30-40 to keep context window and latency compact
    const sampleCandidates = candidates.slice(0, 35).map((v, i) => ({
      idx: i,
      bvid: v.bvid,
      title: v.title,
      author: v.owner?.name || 'UP主',
      source: v.sourceTag || '推荐',
      views: v.stat?.view || 0,
      likes: v.stat?.like || 0,
      desc: (v.desc || '').slice(0, 100),
    }));

    const systemPrompt = `你是一个深谙年轻人与B站生态的内容策展人和AI算法推荐引擎。
你的任务是：根据用户的个人喜好标签、关注的UP主名单、个性化诉求，从候选视频列表中精挑细选出最优质、最契合的 ${limit} 个视频，并为每个视频撰写极具吸引力且个性化的“推荐理由”(rcmd_reason)。

推荐理由撰写规范：
1. 长度在 8 ~ 22 个字之间，精简凝练，直击痛点或亮点；
2. 如果是用户关注的UP主作品，突出UP主或其近期话题（如："关注UP新作: 影视飓风最新画质测评"）；
3. 如果是关注UP相关的衍生视频，点明关联度（如："关注拓展: 类似极客湾的硬核芯片深度解密"）；
4. 如果是契合用户兴趣分类的，点明核心价值（如："AI深度精选: 计算机视觉最新开源模型解析"）；
5. 必须严格输出 JSON 格式，不要包含任何额外的 Markdown 代码块外文字。`;

    const userContent = {
      user_preferences: userPreferences.length > 0 ? userPreferences : ['数码科技', '编程学习', '硬核科普'],
      followed_creators: followedCreators.length > 0 ? followedCreators : ['影视飓风', '极客湾', '老师好我叫何同学'],
      user_custom_prompt: customPrompt || '请结合我的关注列表以及近期最值得看的内容做推荐',
      target_count: limit,
      candidate_videos: sampleCandidates,
      output_schema: {
        recommendations: [
          {
            bvid: "视频BVID",
            rcmd_reason: "精简吸引人的推荐理由",
            match_score: 95,
            tag: "主题标签"
          }
        ]
      }
    };

    const endpoint = this.baseURL.endsWith('/chat/completions')
      ? this.baseURL
      : `${this.baseURL}/chat/completions`;

    const body = {
      model: this.model,
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: JSON.stringify(userContent) },
      ],
      temperature: this.temperature,
      response_format: { type: 'json_object' },
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 20000); // 20s timeout

    const res = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.apiKey}`,
      },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`LLM API returned ${res.status}: ${errText.slice(0, 200)}`);
    }

    const data = await res.json();
    const content = data.choices?.[0]?.message?.content;
    if (!content) {
      throw new Error('LLM response message content was empty.');
    }

    let parsed;
    try {
      // Clean potential markdown backticks
      const cleanJson = content.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
      parsed = JSON.parse(cleanJson);
    } catch (e) {
      console.warn('[Recommender] Failed to parse LLM JSON:', e.message, 'raw:', content);
      return null;
    }

    const list = parsed.recommendations || parsed.list || (Array.isArray(parsed) ? parsed : null);
    return list;
  }

  /**
   * Merge AI recommended items with full VideoItem candidate pool
   */
  mergeRecommendations(candidates, aiResults, limit) {
    const candidateMap = new Map(candidates.map(v => [v.bvid, v]));
    const results = [];
    const usedBvids = new Set();

    for (const item of aiResults) {
      if (!item.bvid) continue;
      const original = candidateMap.get(item.bvid);
      if (original && !usedBvids.has(item.bvid)) {
        usedBvids.add(item.bvid);
        const cloned = { ...original };
        const reason = item.rcmd_reason || item.reason || 'AI智能优选';
        cloned.rcmd_reason = { content: reason };
        cloned.ai_score = item.match_score || 90;
        cloned.ai_tag = item.tag || 'AI推荐';
        results.push(cloned);
      }
    }

    // If AI results were fewer than limit, fill with remaining candidates
    if (results.length < limit) {
      for (const v of candidates) {
        if (!usedBvids.has(v.bvid)) {
          usedBvids.add(v.bvid);
          const cloned = { ...v };
          if (!cloned.rcmd_reason?.content) {
            cloned.rcmd_reason = { content: '热门推荐' };
          }
          results.push(cloned);
          if (results.length >= limit) break;
        }
      }
    }

    return results.slice(0, limit);
  }

  /**
   * Fallback Smart Heuristic Recommender (runs when no API key is provided)
   */
  fallbackHeuristicRecommend({ candidates, userPreferences, followedCreators, customPrompt, limit }) {
    const scored = candidates.map(v => {
      let score = 50; // base score
      const titleLower = (v.title || '').toLowerCase();
      const descLower = (v.desc || '').toLowerCase();
      const authorLower = (v.owner?.name || '').toLowerCase();
      const sourceTag = v.sourceTag || '';

      let matchedReasons = [];

      // 1. Check followed creators
      for (const creator of followedCreators) {
        const cLower = creator.toLowerCase();
        if (authorLower.includes(cLower) || sourceTag.includes(`关注UP[${creator}]直发`)) {
          score += 40;
          matchedReasons.push(`关注UP新作: ${creator}`);
          break;
        } else if (sourceTag.includes(`关注UP[${creator}]相关拓展`)) {
          score += 28;
          matchedReasons.push(`关注拓展: 类似${creator}的作品`);
          break;
        }
      }

      // 2. Check user preference keywords
      for (const pref of userPreferences) {
        const pLower = pref.toLowerCase();
        if (titleLower.includes(pLower) || descLower.includes(pLower)) {
          score += 15;
          matchedReasons.push(`契合偏好: ${pref}`);
          break;
        }
      }

      // 3. Check custom prompt keywords if given
      if (customPrompt) {
        const words = customPrompt.split(/[\s,，、]+/);
        for (const word of words) {
          if (word.length >= 2 && (titleLower.includes(word) || descLower.includes(word))) {
            score += 12;
            matchedReasons.push(`匹配意图: ${word}`);
            break;
          }
        }
      }

      // 4. View count / engagement bonus
      const views = v.stat?.view || 0;
      if (views > 1000000) score += 8;
      else if (views > 100000) score += 4;

      // 5. Build reason
      let finalReason = matchedReasons[0];
      if (!finalReason) {
        if (sourceTag.includes('排行')) finalReason = '全站热榜高赞作品';
        else if (sourceTag.includes('热门')) finalReason = '今日精选热门推荐';
        else finalReason = 'AI智能优选';
      }

      return {
        video: v,
        score,
        reason: finalReason,
      };
    });

    // Sort descending by score
    scored.sort((a, b) => b.score - a.score);

    return scored.slice(0, limit).map(({ video, score, reason }) => {
      const cloned = { ...video };
      cloned.rcmd_reason = { content: reason };
      cloned.ai_score = Math.min(99, Math.max(65, score));
      return cloned;
    });
  }
}

export const recommendationService = new RecommendationService();
