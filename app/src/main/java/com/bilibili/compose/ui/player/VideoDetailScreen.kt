package com.bilibili.compose.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bilibili.compose.ui.home.components.VideoCard
import com.bilibili.compose.ui.player.components.CommentsSection
import com.bilibili.compose.ui.player.components.VideoInfoSection
import com.bilibili.compose.ui.player.components.VideoPartsSection
import com.bilibili.compose.ui.player.components.VideoPlayerView
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    bvid: String,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    onRelatedVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bvid) {
        viewModel.loadVideo(bvid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.videoDetail?.title ?: "视频播放",
                        maxLines = 1,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
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
            // 1. Video Player
            VideoPlayerView(
                videoUrl = uiState.videoStreamUrl,
                isLoading = uiState.isLoadingDetail || uiState.isLoadingStream,
                errorMessage = uiState.errorMessage,
                onRetry = { viewModel.retry() }
            )

            // 2. Video Content Body (Scrollable)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Video Info
                uiState.videoDetail?.let { detail ->
                    item {
                        VideoInfoSection(video = detail)
                    }

                    // Video Parts (if multi-part)
                    if (detail.pages.size > 1) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            VideoPartsSection(
                                pages = detail.pages,
                                selectedCid = uiState.selectedCid,
                                onPartSelect = { page ->
                                    viewModel.selectPart(page.cid)
                                }
                            )
                        }
                    }
                }

                // Tabs: Related vs Comments
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = BiliPink
                    ) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.setTab(0) },
                            text = {
                                Text(
                                    text = "相关推荐",
                                    color = if (uiState.selectedTab == 0) BiliPink else TextSecondary
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.setTab(1) },
                            text = {
                                Text(
                                    text = "评论 (${uiState.comments.size})",
                                    color = if (uiState.selectedTab == 1) BiliPink else TextSecondary
                                )
                            }
                        )
                    }
                }

                if (uiState.selectedTab == 0) {
                    // Related Videos list
                    if (uiState.isLoadingRelated && uiState.relatedVideos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = BiliPink,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else if (uiState.relatedError != null && uiState.relatedVideos.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = uiState.relatedError ?: "加载推荐失败",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { viewModel.reloadRelatedVideos() }
                                ) {
                                    Text(text = "重试", color = BiliPink)
                                }
                            }
                        }
                    } else if (uiState.relatedVideos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无相关推荐",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.relatedVideos,
                            key = { it.bvid.ifBlank { it.aid.toString() } }
                        ) { video ->
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                VideoCard(
                                    video = video,
                                    onClick = { onRelatedVideoClick(video.bvid) }
                                )
                            }
                        }
                    }
                } else {
                    // Comments list
                    item {
                        CommentsSection(
                            comments = uiState.comments,
                            isLoading = uiState.isLoadingComments
                        )
                    }
                }
            }
        }
    }
}
