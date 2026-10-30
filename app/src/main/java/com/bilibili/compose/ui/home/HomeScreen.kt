package com.bilibili.compose.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bilibili.compose.ui.home.components.VideoCard
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVideoClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onPopularClick: () -> Unit,
    onDynamicClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userNav by viewModel.authRepository.userNavFlow.collectAsState(initial = null)
    val isLoggedIn by viewModel.authRepository.isLoggedInFlow.collectAsState(initial = false)

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showCdpDialog by remember { mutableStateOf(false) }
    var showAiServerDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isLoading && uiState.hasMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // AI 推荐 Capsule button
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.isAiMode) BiliPink else BiliPink.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { viewModel.toggleAiMode(!uiState.isAiMode) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI推荐",
                                    tint = if (uiState.isAiMode) Color.White else BiliPink,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "AI推荐",
                                    color = if (uiState.isAiMode) Color.White else BiliPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 热门 Capsule button
                        Surface(
                            shape = CircleShape,
                            color = BiliPink.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onPopularClick)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "热门专区",
                                    tint = BiliPink,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "热门",
                                    color = BiliPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 历史 Capsule button
                        Surface(
                            shape = CircleShape,
                            color = BiliPink.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onHistoryClick)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "历史记录",
                                    tint = BiliPink,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "历史",
                                    color = BiliPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Pink-dot entry for secondary actions
                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { showMoreMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(BiliPink, CircleShape)
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("动态") },
                                leadingIcon = { Icon(Icons.Default.DynamicFeed, contentDescription = null, tint = BiliPink) },
                                onClick = { showMoreMenu = false; onDynamicClick() }
                            )
                            DropdownMenuItem(
                                text = { Text("AI服务配置") },
                                leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = if (uiState.isAiMode) BiliPink else TextSecondary) },
                                onClick = { showMoreMenu = false; showAiServerDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("CDP状态") },
                                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null, tint = TextSecondary) },
                                onClick = { showMoreMenu = false; showCdpDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("刷新") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary) },
                                onClick = { showMoreMenu = false; viewModel.refresh() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isLoggedIn) "个人中心" else "登录/个人中心") },
                                leadingIcon = {
                                    if (isLoggedIn && userNav?.face?.isNotBlank() == true) {
                                        AsyncImage(
                                            model = userNav?.face,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BiliPink)
                                    }
                                },
                                onClick = { showMoreMenu = false; onProfileClick() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // AI Mode active info banner
            if (uiState.isAiMode) {
                Surface(
                    color = Color(0xFFFFF0F5),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BiliPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.aiStatusMessage ?: "已开启 AI 推荐 (${uiState.aiServerUrl})",
                                fontSize = 11.sp,
                                color = BiliPink,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "配置服务",
                            fontSize = 11.sp,
                            color = BiliPink,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showAiServerDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isRefreshing && uiState.videos.isEmpty() -> {
                        // First time loading
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BiliPink)
                        }
                    }

                    uiState.errorMessage != null && uiState.videos.isEmpty() -> {
                        // Error state with retry
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "加载失败",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.refresh() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
                                ) {
                                    Text("重试")
                                }
                                if (uiState.isAiMode) {
                                    OutlinedButton(
                                        onClick = { showAiServerDialog = true }
                                    ) {
                                        Text("更改服务器网址")
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        // Video feed
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = uiState.videos,
                                key = { index, item -> "${item.bvid}_${index}" }
                            ) { _, video ->
                                VideoCard(
                                    video = video,
                                    onClick = { onVideoClick(video.bvid) }
                                )
                            }

                            // Footer loading / bottom indicator
                            if (uiState.isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = BiliPink,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            } else if (!uiState.hasMore && uiState.videos.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "已经到底啦 (・ω・)",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // AI Server Settings dialog
    if (showAiServerDialog) {
        AiServerDialog(
            currentUrl = uiState.aiServerUrl,
            onDismiss = { showAiServerDialog = false },
            onTest = { url, cb -> viewModel.testAiConnection(url, cb) },
            onSave = { newUrl ->
                viewModel.updateAiServerUrl(newUrl)
                if (!uiState.isAiMode) {
                    viewModel.toggleAiMode(true)
                }
                showAiServerDialog = false
            }
        )
    }

    // CDP info dialog
    if (showCdpDialog) {
        CdpStatusDialog(onDismiss = { showCdpDialog = false })
    }
}

@Composable
fun AiServerDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onTest: (String, (Boolean, String) -> Unit) -> Unit,
    onSave: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf(currentUrl) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testing by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = BiliPink,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("连接 AI 推荐服务", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "请扫描 Web 页面展示的二维码，或直接输入服务端网址。服务将根据关注列表及相关拓展视频生成 AI 智能推荐。",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("服务地址 (URL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { clipText ->
                                if (clipText.isNotBlank()) {
                                    inputUrl = clipText.trim()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "粘贴",
                                tint = BiliPink
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick preset addresses
                Text("快捷地址选择:", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { inputUrl = "http://192.168.10.4:3000" },
                        label = { Text("局域网IP", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { inputUrl = "http://10.0.2.2:3000" },
                        label = { Text("模拟器", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { inputUrl = "http://127.0.0.1:3000" },
                        label = { Text("ADB转发", fontSize = 11.sp) }
                    )
                }

                // Test result display
                if (testResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (isSuccess == true) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testResult ?: "",
                            fontSize = 12.sp,
                            color = if (isSuccess == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(inputUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
            ) {
                Text("保存并连接")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        testing = true
                        testResult = "正在测试连接..."
                        isSuccess = null
                        onTest(inputUrl) { success, msg ->
                            testing = false
                            isSuccess = success
                            testResult = msg
                        }
                    },
                    enabled = !testing
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            color = BiliPink,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("测试连接")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
fun CdpStatusDialog(onDismiss: () -> Unit) {
    var testResult by remember { mutableStateOf("点击下方测试 CDP 连接 (端口 9222)") }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edge 浏览器 CDP (端口 9222)") },
        text = {
            Column {
                Text(
                    text = "系统检测到 Edge 正在运行 CDP 调试服务 (端口 9222)，可用于接口抓包测试、校验最新参数以及直接共享浏览器会话。",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = testResult,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    testing = true
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            try {
                                val url = URL("http://10.0.2.2:9222/json/version")
                                val conn = url.openConnection() as HttpURLConnection
                                conn.connectTimeout = 1500
                                conn.readTimeout = 1500
                                val code = conn.responseCode
                                if (code == 200) {
                                    "模拟器连接宿主 CDP (10.0.2.2:9222) 成功！\nHTTP $code"
                                } else {
                                    "连接返回状态码: $code"
                                }
                            } catch (e: Exception) {
                                try {
                                    val url = URL("http://localhost:9222/json/version")
                                    val conn = url.openConnection() as HttpURLConnection
                                    conn.connectTimeout = 1500
                                    conn.readTimeout = 1500
                                    val code = conn.responseCode
                                    "本地 CDP (localhost:9222) 连接成功！HTTP $code"
                                } catch (e2: Exception) {
                                    "提示: Android真机/模拟器访问宿主电脑可配置 ADB 端口转发:\nadb reverse tcp:9222 tcp:9222\n详细: ${e.message}"
                                }
                            }
                        }
                        testResult = res
                        testing = false
                    }
                },
                enabled = !testing,
                colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
            ) {
                if (testing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("测试连接")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
