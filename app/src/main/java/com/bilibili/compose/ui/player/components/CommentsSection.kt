package com.bilibili.compose.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun CommentsHeader(
    totalCount: Int,
    mode: Int,
    onModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val countText = if (totalCount > 0) "共 ${FormatUtils.formatCount(totalCount.toLong())} 条" else "全部评论"
        Text(
            text = countText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "最热",
                fontSize = 12.sp,
                fontWeight = if (mode == 3) FontWeight.Bold else FontWeight.Normal,
                color = if (mode == 3) BiliPink else TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onModeChange(3) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Text(
                text = "|",
                fontSize = 12.sp,
                color = Color(0xFFDDDDDD)
            )

            Text(
                text = "最新",
                fontSize = 12.sp,
                fontWeight = if (mode == 2) FontWeight.Bold else FontWeight.Normal,
                color = if (mode == 2) BiliPink else TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onModeChange(2) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun CommentItemView(
    comment: ReplyItem,
    upMid: Long? = null,
    onOpenSubReplies: (ReplyItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.member?.avatar?.ifBlank { null },
            contentDescription = comment.member?.uname,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAEAEA)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // User name & badges & like count
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
                    val nameColor = if (isVip) BiliPink else TextSecondary

                    Text(
                        text = comment.member?.uname ?: "B站用户",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = nameColor,
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

                    // UP Badge
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

                // Likes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
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

            Spacer(modifier = Modifier.height(4.dp))

            // Main comment text
            Text(
                text = comment.content?.message ?: "",
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Time & Reply Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeText = comment.replyControl?.timeDesc?.ifBlank { null }
                    ?: if (comment.ctime > 0) FormatUtils.formatDate(comment.ctime) else ""
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    color = TextMuted
                )

                // Clickable reply button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onOpenSubReplies(comment) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "回复",
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (comment.rcount > 0) "${comment.rcount} 回复" else "回复",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Sub-replies Preview
            val subReplies = comment.replies
            if (!subReplies.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenSubReplies(comment) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        subReplies.take(2).forEach { sub ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${sub.member?.uname ?: "用户"}: ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = sub.content?.message ?: "",
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val subCountText = comment.replyControl?.subReplyEntryText
                            ?: if (comment.rcount > subReplies.size) "共 ${comment.rcount} 条回复 >" else "查看回复 >"

                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onOpenSubReplies(comment) }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = subCountText,
                                fontSize = 11.sp,
                                color = BiliPink,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsFooter(
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = BiliPink,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在加载更多评论...",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            error != null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { onLoadMore() }
                ) {
                    Text(
                        text = "加载失败: $error，点击重试",
                        color = BiliPink,
                        fontSize = 12.sp
                    )
                }
            }
            !hasMore -> {
                Text(
                    text = "— 没有更多评论了 —",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
            else -> {
                TextButton(onClick = onLoadMore) {
                    Text(
                        text = "点击加载更多评论",
                        color = BiliPink,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Standalone CommentsSection for backward-compatibility or previews.
 */
@Composable
fun CommentsSection(
    comments: List<ReplyItem>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    totalCount: Int = 0,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    errorMessage: String? = null,
    onLoadMore: () -> Unit = {},
    mode: Int = 3,
    onModeChange: (Int) -> Unit = {},
    onOpenSubReplies: (ReplyItem) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (comments.isNotEmpty() || totalCount > 0) {
            CommentsHeader(
                totalCount = totalCount,
                mode = mode,
                onModeChange = onModeChange
            )
        }

        if (isLoading && comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BiliPink, modifier = Modifier.size(28.dp))
            }
        } else if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (errorMessage != null) "评论加载失败: $errorMessage" else "暂无评论，快来抢沙发吧~",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            Column {
                comments.forEach { comment ->
                    CommentItemView(
                        comment = comment,
                        onOpenSubReplies = onOpenSubReplies
                    )
                }

                CommentsFooter(
                    isLoadingMore = isLoadingMore,
                    hasMore = hasMore,
                    error = errorMessage,
                    onLoadMore = onLoadMore
                )
            }
        }
    }
}
