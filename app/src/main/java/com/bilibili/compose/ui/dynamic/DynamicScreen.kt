package com.bilibili.compose.ui.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bilibili.compose.ui.dynamic.components.CreatorFilterBar
import com.bilibili.compose.ui.dynamic.components.DynamicCard
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicScreen(
    viewModel: DynamicViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by viewModel.authRepository.isLoggedInFlow.collectAsState(initial = false)
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCdpDialog by remember { mutableStateOf(false) }

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isLoading && uiState.hasMore) {
            viewModel.loadMore()
        }
    }

    // Show CDP message in snackbar if available
    LaunchedEffect(uiState.cdpMessage) {
        uiState.cdpMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearCdpMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "动态",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (uiState.selectedCreator != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BiliPink.copy(alpha = 0.12f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = uiState.selectedCreator?.uname.orEmpty(),
                                    color = BiliPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Edge CDP Sync button
                    IconButton(onClick = { showCdpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Edge 9222 同步",
                            tint = if (uiState.cdpSyncLoading) BiliPink else TextSecondary
                        )
                    }

                    // Refresh
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Unauthenticated CDP Quick Sync Banner
            if (!isLoggedIn) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BiliPink.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "CDP 同步",
                            tint = BiliPink,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "检测到 Edge 浏览器 (端口 9222)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "点击一键从 Edge 浏览器动态标签页同步登录状态与关注列表",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.syncWithEdgeCdp(9222) },
                            enabled = !uiState.cdpSyncLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = BiliPink),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (uiState.cdpSyncLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("一键同步", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Top Creator Filter Bar
            CreatorFilterBar(
                creators = uiState.filteredCreators,
                selectedCreator = uiState.selectedCreator,
                onSelectCreator = { creator ->
                    viewModel.selectCreator(creator)
                },
                searchQuery = uiState.creatorSearchQuery,
                onSearchQueryChange = { query ->
                    viewModel.onCreatorSearchQueryChange(query)
                },
                isSearchActive = uiState.isSearchActive,
                onToggleSearch = { active ->
                    viewModel.toggleSearchActive(active)
                }
            )

            // Dynamic Feeds Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.dynamics.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BiliPink)
                        }
                    }

                    uiState.errorMessage != null && uiState.dynamics.isEmpty() -> {
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
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
                            ) {
                                Text("重试")
                            }
                        }
                    }

                    uiState.dynamics.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (uiState.selectedCreator != null) {
                                    "创作者「${uiState.selectedCreator?.uname}」暂无动态"
                                } else {
                                    "暂无动态，请尝试同步 Edge 浏览器或登录"
                                },
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            if (uiState.selectedCreator != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(onClick = { viewModel.selectCreator(null) }) {
                                    Text("查看全部动态")
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.dynamics, key = { it.idStr.ifBlank { it.hashCode().toString() } }) { item ->
                                DynamicCard(
                                    item = item,
                                    onVideoClick = onVideoClick,
                                    onCreatorClick = { mid, uname ->
                                        val matching = uiState.creators.find { it.mid == mid }
                                        viewModel.selectCreator(matching ?: com.bilibili.compose.data.model.DynamicCreator(mid = mid, uname = uname))
                                    }
                                )
                            }

                            // Loading more indicator
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = BiliPink,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            } else if (!uiState.hasMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "— 没有更多动态了 —",
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

    // CDP 9222 Sync Dialog
    if (showCdpDialog) {
        AlertDialog(
            onDismissRequest = { showCdpDialog = false },
            title = { Text("Edge 浏览器 CDP (端口 9222)") },
            text = {
                Column {
                    Text(
                        text = "当前 Edge 浏览器开启了端口 9222 CDP 调试服务，并正在运行 B 站动态页面。\n\n点击“立即同步”即可直接从 Edge 提取最新 SESSDATA 和 Cookie 凭据，自动加载创作者关注列表和最新动态。",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCdpDialog = false
                        viewModel.syncWithEdgeCdp(9222)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BiliPink)
                ) {
                    Text("立即同步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCdpDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}
