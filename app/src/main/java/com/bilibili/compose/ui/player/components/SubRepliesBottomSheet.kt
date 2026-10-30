package com.bilibili.compose.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bilibili.compose.data.model.ReplyItem
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextMuted
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary
import com.bilibili.compose.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubRepliesBottomSheet(
    rootComment: ReplyItem,
    subReplies: List<ReplyItem>,
    totalCount: Int,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    upMid: Long?,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetListState = rememberLazyListState()

    // Auto load more when scrolling near bottom
    val shouldLoadMore = remember {
        derivedStateOf {
            val total = sheetListState.layoutInfo.totalItemsCount
            val lastVisible = sheetListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 2
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoading && !isLoadingMore && hasMore) {
            onLoadMore()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerTitle = if (totalCount > 0) {
                    "回复详情 (${FormatUtils.formatCount(totalCount.toLong())})"
                } else {
                    "回复详情"
                }
                Text(
                    text = headerTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Body
            LazyColumn(
                state = sheetListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Root Comment Item
                item {
                    RootCommentHeader(
                        comment = rootComment,
                        upMid = upMid,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider(
                        thickness = 6.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                }

                // Sub-replies Section Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "全部回复",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }

                // Sub-replies items
                if (isLoading && subReplies.isEmpty()) {
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
                } else if (error != null && subReplies.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "加载回复失败: $error",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = onLoadMore) {
                                Text(text = "重试", color = BiliPink)
                            }
                        }
                    }
                } else if (subReplies.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无更多回复",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = subReplies,
                        key = { it.rpid }
                    ) { reply ->
                        SubReplyItemView(
                            reply = reply,
                            upMid = upMid,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    }

                    // Footer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isLoadingMore -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = BiliPink,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "正在加载更多回复...",
                                            color = TextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                error != null -> {
                                    Text(
                                        text = "加载失败，点击重试",
                                        color = BiliPink,
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable { onLoadMore() }
                                    )
                                }
                                !hasMore -> {
                                    Text(
                                        text = "— 没有更多回复了 —",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                                else -> {
                                    TextButton(onClick = onLoadMore) {
                                        Text(
                                            text = "加载更多回复",
                                            color = BiliPink,
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
}

@Composable
private fun RootCommentHeader(
    comment: ReplyItem,
    upMid: Long?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.member?.avatar?.ifBlank { null },
            contentDescription = comment.member?.uname,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAEAEA)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val isVip = comment.member?.vip?.vipStatus == 1
                    Text(
                        text = comment.member?.uname ?: "B站用户",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isVip) BiliPink else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Top badge
                    if (comment.isTop) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = BiliPink.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "置顶",
                                fontSize = 10.sp,
                                color = BiliPink,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // UP badge
                    if (upMid != null && upMid > 0 && comment.mid == upMid) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = BiliPink
                        ) {
                            Text(
                                text = "UP主",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Level Badge
                    val level = comment.member?.levelInfo?.currentLevel ?: 0
                    if (level > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = when (level) {
                                in 1..2 -> Color(0xFF9E9E9E)
                                in 3..4 -> Color(0xFF64B5F6)
                                in 5..6 -> Color(0xFFFF9800)
                                else -> Color(0xFFBDBDBD)
                            }.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Lv.$level",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (level) {
                                    in 1..2 -> Color(0xFF757575)
                                    in 3..4 -> Color(0xFF1976D2)
                                    in 5..6 -> Color(0xFFF57C00)
                                    else -> Color(0xFF757575)
                                },
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "赞",
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = FormatUtils.formatCount(comment.like.toLong()),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = comment.content?.message ?: "",
                fontSize = 15.sp,
                color = TextPrimary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            val timeText = comment.replyControl?.timeDesc?.ifBlank { null }
                ?: if (comment.ctime > 0) FormatUtils.formatDate(comment.ctime) else ""
            if (timeText.isNotBlank()) {
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun SubReplyItemView(
    reply: ReplyItem,
    upMid: Long?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = reply.member?.avatar?.ifBlank { null },
            contentDescription = reply.member?.uname,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAEAEA)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val isVip = reply.member?.vip?.vipStatus == 1
                    Text(
                        text = reply.member?.uname ?: "B站用户",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = if (isVip) BiliPink else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (upMid != null && upMid > 0 && reply.mid == upMid) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = BiliPink
                        ) {
                            Text(
                                text = "UP主",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                            )
                        }
                    }

                    val level = reply.member?.levelInfo?.currentLevel ?: 0
                    if (level > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = when (level) {
                                in 1..2 -> Color(0xFF9E9E9E)
                                in 3..4 -> Color(0xFF64B5F6)
                                in 5..6 -> Color(0xFFFF9800)
                                else -> Color(0xFFBDBDBD)
                            }.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Lv.$level",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (level) {
                                    in 1..2 -> Color(0xFF757575)
                                    in 3..4 -> Color(0xFF1976D2)
                                    in 5..6 -> Color(0xFFF57C00)
                                    else -> Color(0xFF757575)
                                },
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "赞",
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = FormatUtils.formatCount(reply.like.toLong()),
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = reply.content?.message ?: "",
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            val timeText = reply.replyControl?.timeDesc?.ifBlank { null }
                ?: if (reply.ctime > 0) FormatUtils.formatDate(reply.ctime) else ""
            if (timeText.isNotBlank()) {
                Text(
                    text = timeText,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
