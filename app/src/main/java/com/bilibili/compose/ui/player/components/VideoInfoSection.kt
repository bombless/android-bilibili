package com.bilibili.compose.ui.player.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bilibili.compose.data.model.VideoViewData
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextMuted
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary
import com.bilibili.compose.util.FormatUtils

@Composable
fun VideoInfoSection(
    video: VideoViewData,
    isFollowing: Boolean? = null,
    isUpdatingFollow: Boolean = false,
    followError: String? = null,
    onFollowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isDescExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // UP Host Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = video.owner?.face?.ifBlank { null },
                contentDescription = video.owner?.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.owner?.name ?: "UP主",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "发布于 " + FormatUtils.formatDate(video.pubdate),
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isFollowing == true) MaterialTheme.colorScheme.surfaceVariant else BiliPink,
                modifier = Modifier.clickable(enabled = !isUpdatingFollow && isFollowing != null, onClick = onFollowClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isUpdatingFollow) CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    Text(
                        text = when { isUpdatingFollow -> "处理中"; isFollowing == null -> "加载中"; isFollowing -> "已关注"; else -> "+ 关注" },
                        color = if (isFollowing == true) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        followError?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title and expand arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isDescExpanded = !isDescExpanded },
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = video.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = if (isDescExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isDescExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = "展开简介",
                tint = TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // View and Danmaku counts
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${FormatUtils.formatCount(video.stat?.view ?: 0)} 观看",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = "${FormatUtils.formatCount(video.stat?.danmaku ?: 0)} 弹幕",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = video.bvid,
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        // Expandable Description
        if (isDescExpanded && video.desc.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = video.desc,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.animateContentSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions: Like, Coin, Favorite, Share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ActionItem(
                icon = Icons.Default.ThumbUp,
                label = FormatUtils.formatCount(video.stat?.like ?: 0)
            )
            ActionItem(
                icon = Icons.Default.Favorite,
                label = FormatUtils.formatCount(video.stat?.coin ?: 0)
            )
            ActionItem(
                icon = Icons.Default.Stars,
                label = FormatUtils.formatCount(video.stat?.favorite ?: 0)
            )
            ActionItem(
                icon = Icons.Default.Share,
                label = FormatUtils.formatCount(video.stat?.share ?: 0)
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}
