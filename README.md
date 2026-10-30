# 哔哩哔哩 (Bilibili) Android 客户端

基于 **Kotlin + Jetpack Compose + Material 3** 构建的现代化 Bilibili 客户端，支持账号扫码登录、首页热门视频流推荐、ExoPlayer 视频高清流畅播放、多分P切换、相关视频推荐以及视频评论。

同时集成了 **Edge 浏览器 CDP (Chrome DevTools Protocol) 联动调试机制**（默认端口 `9222`），可供接口参考、抓包验证与 Cookie 状态快速测试。

---

## 🌟 功能特性

### 1. 首页视频推荐与热门专区 (`HomeScreen` & `PopularScreen`)
- **首页推荐流 (`HomeScreen`)**：
  - 采用 B 站官方推荐 API (`/x/web-interface/index/top/feed/rcmd`)，智能无限下拉流；
  - 顶部显著位置提供“热门”胶囊入口按钮（带火焰标识），点击无缝跳转至独立热门专区；
- **热门专区 (`PopularScreen`)**：
  - **综合热门**：采用 `/x/web-interface/popular`，支持分页与无限加载，展示热度标签（如“人气飙升”、“万赞”等）；
  - **排行榜**：采用 `/x/web-interface/ranking`，展示全站 TOP 100 排行，并区分金银铜牌（TOP 1/2/3）专属高光标识；
  - **入站必刷**：采用 `/x/web-interface/popular/precious`，收录全网经典殿堂级作品，突出展示专属成就勋章；
- 视频卡片优雅展示：16:9 高清封面（自动处理 Anti-Hotlink 鉴权防盗链）、播放量与弹幕数统计、视频时长标签、UP主信息标签；
- 支持下拉刷新与滑到底部自动加载更多。

### 2. 动态广场与创作者账号过滤 (`DynamicScreen`)
- **Edge 浏览器 CDP (端口 9222) 深度联动**：
  - 自动识别宿主机端口 9222 的 Edge 调试服务及打开的 B 站动态标签页；
  - 一键通过 WebSocket CDP 协议提取会话 Cookie (`SESSDATA`、`bili_jct`、`DedeUserID` 等)，免扫码无缝同步 Edge 浏览器的登录与动态数据；
- **创作者账号 (UP主) 筛选器 (`CreatorFilterBar`)**：
  - 顶部水平滑动创作者栏，展示关注的 UP 主头像、昵称，并带有实时动态更新红点；
  - **全部动态**与**指定创作者动态**一键无缝切换：点击指定 UP 主即可直接过滤该创作者的所有动态；
  - 支持创作者名称实时搜索匹配，快捷定位目标 UP 主；
  - 具有明确的高亮选中态和可一键清除的激活筛选胶囊；
- **多样化动态卡片 (`DynamicCard`)**：
  - **投稿视频动态**：展示高清 16:9 封面、播放/弹幕统计、时长标签与标题，点击直接无缝跳转至播放器播放；
  - **图文相册动态**：展示单图大图及多图九宫格流，适配 Coil 图片鉴权加载；
  - **转发/转发动态**：优雅嵌套原动态卡片与原作者高光；
  - **互动状态条**：展示转发量、评论量与点赞数。

### 3. 视频详情与多媒体播放 (`VideoDetailScreen`)
- 基于 **AndroidX Media3 (ExoPlayer)** 实现；
- **防盗链优化**：配置专门的 `DefaultHttpDataSource`，注入合法的 `Referer: https://www.bilibili.com/` 与浏览器 `User-Agent`，解决 B 站 CDN 403 访问限制；
- **分P支持**：针对多分P视频（`pages.size > 1`）自动展示水平选集栏，点击瞬间无缝切换分P播放；
- **画质支持**：支持解析清晰度列表（720P、360P 等）；
- **相关视频推荐与评论联动 (`CommentsSection` & `SubRepliesBottomSheet`)**：
  - 点击“相关推荐”中的卡片可直接切换至新视频；
  - 切换至“评论”标签采用现代化 B 站游标分页接口 (`/x/v2/reply/main`)，解决旧接口只能加载 20 条评论的限制，支持无限向下滚动流式加载更多；
  - 支持置顶评论高光标识、UP主作者标识、用户等级徽章与热评点赞展示；
  - 支持按“最热”与“最新”两种排序模式一键无缝切换；
  - **楼中楼二级回复支持进入与流式加载**：点击主评卡片、子回复卡片或“共X条回复”可直接唤起二级回复详情抽屉 (`SubRepliesBottomSheet`)，支持楼主信息置顶、楼中楼全部回复分页加载与无限滚动。

### 4. 账号登录系统 (`LoginScreen`)
- **二维码扫码登录**：
  - 接入 B 站官方 Passport 二维码登录 API (`/x/passport-login/web/qrcode/generate`)；
  - 本地使用 ZXing 动态生成清晰二维码；
  - 自动轮询登录状态（未扫码、已扫码待确认、已失效、登录成功）；
  - 登录成功自动捕获并持久化存储 `SESSDATA`、`bili_jct`、`DedeUserID` 等凭据。
- **Edge 浏览器 CDP 联动 (端口 9222)**：
  - 配合宿主运行在 9222 端口的 Edge 浏览器，提供 CDP 测试与会话同步入口。
- **手动输入 Cookie / SESSDATA**：
  - 为开发者及高级用户提供直接粘贴 Cookie 的快捷方案。

### 5. 个人中心 (`ProfileScreen`)
- 登录后展示：用户头像、昵称、UID、硬币余额、B币余额、大会员及等级（Lv.1 ~ Lv.6）；
- 未登录状态提供一键登录引导；
- 支持一键安全退出登录。

### 6. AI 智能推荐服务端与客户端联动 (`server/` & `HomeScreen`)
- **Node.js 免登录视频抓取与候选池构建 (`server/services/bilibili.js`)**：
  - **免登录全网抓取**：无需用户登录凭证，支持抓取全站综合热门、分区排行榜、首页推荐流、兴趣搜索新作品；
  - **关注列表直发抓取**：针对关注的 UP 主（如影视飓风、极客湾、何同学、稚晖君等）自动抓取近期最新作品；
  - **关注视频相关拓展 (`archive/related`)**：对关注 UP 主的视频自动拓展抓取深度关联的推荐视频候选池；
- **Chat Completions 大模型深度分析与喜好推荐 (`server/services/recommender.js`)**：
  - 支持接入标准 OpenAI 兼容接口（OpenAI、DeepSeek、Moonshot、SiliconFlow、本地 Ollama 等）；
  - 分析用户喜好标签、关注名单与当下诉求，从候选池中智能精选匹配度最高的视频；
  - 为每个推荐视频撰写极具吸引力的个性化推荐理由（如：“关注UP新作”、“关注拓展: 类似极客湾的硬核芯片深度解密”、“AI深度精选”等）；
  - 内置智能启发式规则降级引擎，在未配置 API Key 时仍可秒级提供高质量匹配推荐；
- **Web 可视化控制台与动态二维码展示 (`server/public/index.html`)**：
  - 浏览器端自动提取当前访问网址（`window.location.origin`）动态绘制矢量二维码；
  - 智能感知局域网 IPv4 地址（如 `192.168.x.x`）并提供一键切换与复制，方便手机与模拟器扫码直连；
  - 提供实时推荐流测试、在线播放跳转、耗时与模型状态监控；
  - 支持在 Web 界面实时调整 LLM API Key、Base URL、模型名称、关注 UP 名单与喜好标签；
- **Android 客户端无缝接入**：
  - `HomeScreen` 顶部导航栏新增“**AI推荐**”专属胶囊开关与“AI服务配置”弹窗；
  - 支持手动输入/粘贴服务端地址，提供局域网 IP、模拟器 (`10.0.2.2:3000`)、ADB 端口转发一键预设；
  - 视频卡片优雅渲染橙色高亮 AI 推荐标签，直观呈现推荐原因。

---

## 🏗 技术栈与架构

- **语言**: Kotlin 2.0+
- **UI 框架**: Jetpack Compose (Material 3)
- **网络层**: Retrofit 2 + OkHttp 3 + Gson
- **图片加载**: Coil Compose (配置自定义 Referer 拦截器)
- **视频播放**: AndroidX Media3 (ExoPlayer 1.4.1)
- **本地存储**: Jetpack DataStore (Preferences)
- **条码生成**: Google ZXing Core 3.5.3
- **架构**: 经典 MVVM 架构，分层清晰 (data/model, data/api, data/local, data/repository, ui/*)

---

## 🛠 编译与运行

### 启动 AI 推荐服务 (Node.js)
```bash
# 进入 server 目录并启动
cd server
npm install
npm start
# 或者开发模式热重载
npm run dev
```
启动后访问 Web 控制台：
- 本地浏览器：`http://localhost:3000`
- 局域网访问（手机扫码）：`http://<电脑局域网IP>:3000`（网页会自动识别并展示对应二维码）
- 模拟器访问地址：`http://10.0.2.2:3000`

### 编译 Debug APK
```bash
# 在项目根目录下执行
./gradlew assembleDebug
```
生成的 APK 位于：
`app/build/outputs/apk/debug/app-debug.apk`

### 安装到设备
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### CDP 调试与端口映射 (端口 9222)
若在模拟器或真机上测试宿主机 Edge 浏览器的 CDP 调试服务（宿主机启动参数 `--remote-debugging-port=9222`）：
```bash
# 真机通过 USB 调试时映射端口
adb reverse tcp:9222 tcp:9222

# 运行终端测试脚本
node tools/test_app_api.mjs
```
