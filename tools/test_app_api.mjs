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
  console.log(`\n[2] Testing Popular Videos API...`);
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
    console.log(`    Top Video: [${v.bvid}] ${v.title} by ${v.owner?.name}`);
    return v;
  }
}

async function testPlayUrl(bvid, cid) {
  console.log(`\n[3] Testing Play URL API for [${bvid}], cid=${cid}...`);
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
  console.log(`\n[4] Testing QR Code Login Generation...`);
  const res = await fetch('https://passport.bilibili.com/x/passport-login/web/qrcode/generate', {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  const data = await res.json();
  console.log(`    QR Gen code: ${data.code}, key: ${data.data?.qrcode_key}`);
}

async function main() {
  await checkCdp();
  const v = await testPopular();
  if (v) {
    await testPlayUrl(v.bvid, v.cid);
  }
  await testQrLogin();
}

main().catch(console.error);
