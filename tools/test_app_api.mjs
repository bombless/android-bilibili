/**
 * Bilibili Client API & CDP Testing Tool
 * Connects to Edge Browser running on CDP port 9222
 * or tests APIs directly.
 */

const CDP_PORT = 9222;

async function checkCdp() {
  console.log(`[1] Testing CDP connection on http://localhost:${CDP_PORT}...`);
  try {
    const res = await fetch(`http://localhost:${CDP_PORT}/json/list`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const pages = await res.json();
    console.log(`    CDP is active! Found ${pages.length} targets:`);
    pages.forEach(p => console.log(`    - [${p.type}] ${p.title} (${p.url})`));
    return pages;
  } catch (e) {
    console.log(`    CDP not reachable on port ${CDP_PORT}: ${e.message}`);
    return null;
  }
}

async function testPopular() {
  console.log(`\n[2] Testing Popular Videos API (综合热门)...`);
  const res = await fetch('https://api.bilibili.com/x/web-interface/popular?pn=1&ps=5', {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  const data = await res.json();
  console.log(`    Response code: ${data.code}, items returned: ${data.data?.list?.length}`);
  if (data.data?.list?.[0]) {
    const v = data.data.list[0];
    console.log(`    Top Popular Video: [${v.bvid}] ${v.title} by ${v.owner?.name}`);
    return v;
  }
}

async function testRanking() {
  console.log(`\n[3] Testing Ranking API (排行榜)...`);
  const res = await fetch('https://api.bilibili.com/x/web-interface/ranking?rid=0&day=3', {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  const data = await res.json();
  console.log(`    Response code: ${data.code}, items returned: ${data.data?.list?.length}`);
  if (data.data?.list?.[0]) {
    const v = data.data.list[0];
    console.log(`    Top Ranked Video: [${v.bvid}] ${v.title} by ${v.author} (play: ${v.play})`);
  }
}

async function testPrecious() {
  console.log(`\n[4] Testing Precious API (入站必刷)...`);
  const res = await fetch('https://api.bilibili.com/x/web-interface/popular/precious?page=1&page_size=5', {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  const data = await res.json();
  console.log(`    Response code: ${data.code}, items returned: ${data.data?.list?.length}`);
  if (data.data?.list?.[0]) {
    const v = data.data.list[0];
    console.log(`    Must-Watch Video: [${v.bvid}] ${v.title} (achievement: ${v.achievement})`);
  }
}

async function testRecommend() {
  console.log(`\n[5] Testing Recommend Feed API (首页推荐)...`);
  const res = await fetch('https://api.bilibili.com/x/web-interface/index/top/feed/rcmd?ps=5&fresh_idx=1', {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  const data = await res.json();
  console.log(`    Response code: ${data.code}, items returned: ${data.data?.item?.length}`);
  if (data.data?.item?.[0]) {
    const v = data.data.item[0];
    console.log(`    Recommended Video: [${v.bvid}] ${v.title} by ${v.owner?.name}`);
  }
}

async function testPlayUrl(bvid, cid) {
  console.log(`\n[6] Testing Play URL API for [${bvid}], cid=${cid}...`);
  const res = await fetch(`https://api.bilibili.com/x/player/playurl?bvid=${bvid}&cid=${cid}&qn=64&fnval=1`, {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  const data = await res.json();
  console.log(`    Response code: ${data.code}`);
  console.log(`    Supported qualities:`, data.data?.accept_description);
  console.log(`    Direct Stream URL exists:`, Boolean(data.data?.durl?.[0]?.url));
}

async function testQrLogin() {
  console.log(`\n[7] Testing QR Code Login Generation...`);
  const res = await fetch('https://passport.bilibili.com/x/passport-login/web/qrcode/generate', {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  const data = await res.json();
  console.log(`    QR Gen code: ${data.code}, key: ${data.data?.qrcode_key}`);
}

async function testRepliesWithPagination(bvid, aid) {
  console.log(`\n[8] Testing Video Replies API & Pagination for [${bvid}], aid=${aid}...`);
  const headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Referer': 'https://www.bilibili.com/'
  };

  // Page 1 (next = 0)
  const res1 = await fetch(`https://api.bilibili.com/x/v2/reply/main?oid=${aid}&type=1&mode=3&next=0`, { headers });
  const data1 = await res1.json();
  const replies1 = data1.data?.replies || [];
  const cursor1 = data1.data?.cursor;
  console.log(`    Page 1: code=${data1.code}, replies count: ${replies1.length}, total count: ${cursor1?.all_count}, next cursor: ${cursor1?.next}, is_end: ${cursor1?.is_end}`);

  if (cursor1 && !cursor1.is_end && cursor1.next > 0) {
    // Page 2 (next = cursor.next)
    const res2 = await fetch(`https://api.bilibili.com/x/v2/reply/main?oid=${aid}&type=1&mode=3&next=${cursor1.next}`, { headers });
    const data2 = await res2.json();
    const replies2 = data2.data?.replies || [];
    const cursor2 = data2.data?.cursor;
    console.log(`    Page 2: code=${data2.code}, replies count: ${replies2.length}, next cursor: ${cursor2?.next}, is_end: ${cursor2?.is_end}`);
    console.log(`    Total loaded across 2 pages: ${replies1.length + replies2.length} comments (breaks past 20 items limit!)`);
  }

  // Test Sub-replies (replies to a reply)
  const commentWithSub = replies1.find(r => (r.rcount || 0) > 0);
  if (commentWithSub) {
    console.log(`    Testing sub-replies for comment rpid=${commentWithSub.rpid} (by ${commentWithSub.member?.uname}, sub-count: ${commentWithSub.rcount})...`);
    const subRes = await fetch(`https://api.bilibili.com/x/v2/reply/reply?type=1&oid=${aid}&root=${commentWithSub.rpid}&pn=1&ps=20`, { headers });
    const subData = await subRes.json();
    console.log(`    Sub-replies API: code=${subData.code}, items count: ${subData.data?.replies?.length}, total: ${subData.data?.page?.count}`);
    if (subData.data?.replies?.[0]) {
      console.log(`      First sub-reply: [${subData.data.replies[0].member?.uname}] ${subData.data.replies[0].content?.message?.slice(0, 30)}`);
    }
  }
}

async function testDynamicWithCdp(pages) {
  console.log(`\n[9] Testing Dynamic & Creator Filtering with Edge CDP (port 9222)...`);
  const biliPage = pages?.find(p => p.url?.includes('bilibili.com') || p.title?.includes('动态'));
  if (!biliPage || !biliPage.webSocketDebuggerUrl) {
    console.log('    No Bilibili dynamic page found on CDP.');
    return;
  }

  const ws = new WebSocket(biliPage.webSocketDebuggerUrl);
  await new Promise(r => ws.onopen = r);

  const cookies = await new Promise(resolve => {
    ws.onmessage = (e) => {
      resolve(JSON.parse(e.data).result?.cookies || []);
    };
    ws.send(JSON.stringify({
      id: 1,
      method: 'Network.getCookies',
      params: { urls: ['https://bilibili.com', 'https://api.bilibili.com', 'https://t.bilibili.com'] }
    }));
  });
  ws.close();

  const cookieStr = cookies.map(c => `${c.name}=${c.value}`).join('; ');
  console.log(`    Retrieved ${cookies.length} cookies from Edge!`);

  // Test Portal
  const portalRes = await fetch('https://api.bilibili.com/x/polymer/web-dynamic/v1/portal?up_list_more=1', {
    headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': 'https://t.bilibili.com/', 'Cookie': cookieStr }
  });
  const portalData = await portalRes.json();
  const upCount = portalData.data?.up_list?.items?.length || 0;
  console.log(`    Portal Response: code ${portalData.code}, creators followed: ${upCount}`);
  if (upCount > 0) {
    console.log(`    Top creators:`, portalData.data.up_list.items.slice(0, 3).map(u => u.uname));
  }

  // Test Feed
  const feedRes = await fetch('https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all?timezone_offset=-480&type=all&page=1', {
    headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': 'https://t.bilibili.com/', 'Cookie': cookieStr }
  });
  const feedData = await feedRes.json();
  console.log(`    All Dynamics: code ${feedData.code}, items returned: ${feedData.data?.items?.length}`);

  // Test Space Dynamic (filtering by top creator)
  if (upCount > 0) {
    const topCreator = portalData.data.up_list.items[0];
    const spaceRes = await fetch(`https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space?host_mid=${topCreator.mid}&timezone_offset=-480&page=1`, {
      headers: { 'User-Agent': 'Mozilla/5.0', 'Referer': 'https://t.bilibili.com/', 'Cookie': cookieStr }
    });
    const spaceData = await spaceRes.json();
    console.log(`    Filtered Dynamics for [${topCreator.uname}]: items count ${spaceData.data?.items?.length}`);
  }
}

async function main() {
  const pages = await checkCdp();
  const v = await testPopular();
  await testRanking();
  await testPrecious();
  await testRecommend();
  if (v) {
    await testPlayUrl(v.bvid, v.cid);
    await testRepliesWithPagination(v.bvid, v.aid);
  }
  await testQrLogin();
  if (pages) {
    await testDynamicWithCdp(pages);
  }
}

main().catch(console.error);
