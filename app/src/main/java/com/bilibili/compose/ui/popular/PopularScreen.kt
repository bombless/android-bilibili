package com.bilibili.compose.ui.popular

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bilibili.compose.data.model.PopularTab
import com.bilibili.compose.ui.home.components.VideoCard
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopularScreen(
    viewModel: PopularViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Infinite scroll detection for OVERALL tab
    val shouldLoadMore = remember {
        derivedStateOf {
            if (uiState.currentTab != PopularTab.OVERALL) return@derivedStateOf false
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !uiState.isOverallLoading && uiState.hasMoreOverall) {
            viewModel.loadMore()
        }
    }

    // Scroll to top when tab changes
    LaunchedEffect(uiState.currentTab) {
        listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = BiliPink,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "热门专区",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
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

                // Sub-category Tabs: 综合热门 / 排行榜 / 入站必刷
                TabRow(
                    selectedTabIndex = uiState.currentTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BiliPink,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.currentTab.ordinal]),
                            color = BiliPink
                        )
                    }
                ) {
                    PopularTab.entries.forEach { tab ->
                        val selected = uiState.currentTab == tab
                        Tab(
                            selected = selected,
                            onClick = { viewModel.switchTab(tab) },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) BiliPink else TextSecondary
                                )
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val currentVideos = uiState.currentVideos
            val isLoading = uiState.isCurrentLoading
            val isRefreshing = uiState.isCurrentRefreshing
            val errorMessage = uiState.currentError

            when {
                isRefreshing && currentVideos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BiliPink)
                    }
                }

                errorMessage != null && currentVideos.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage,
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

                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = currentVideos,
                            key = { index, item ->
                                if (item.bvid.isNotBlank()) "${item.bvid}_${item.aid}_${uiState.currentTab}_$index"
                                else "${item.aid}_$index"
                            }
                        ) { _, video ->
                            VideoCard(
                                video = video,
                                onClick = { onVideoClick(video.bvid) }
                            )
                        }

                        // Footer for OVERALL tab (infinite scroll)
                        if (uiState.currentTab == PopularTab.OVERALL) {
                            if (uiState.isOverallLoading) {
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
                            } else if (!uiState.hasMoreOverall && currentVideos.isNotEmpty()) {
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
                        } else if (currentVideos.isNotEmpty()) {
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
