package com.bilibili.compose.ui.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ThumbUp
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
import com.bilibili.compose.data.model.DynamicArchive
import com.bilibili.compose.data.model.DynamicItem
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary

@Composable
fun DynamicCard(
    item: DynamicItem,
    onVideoClick: (String) -> Unit,
    onCreatorClick: ((Long, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Author Row
            val author = item.modules?.moduleAuthor
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onCreatorClick != null && author != null) {
                            Modifier.clickable { onCreatorClick(author.mid, author.name) }
                        } else Modifier
                    )
            ) {
                AsyncImage(
                    model = author?.face,
                    contentDescription = author?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = author?.name ?: "B站用户",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (!author?.pubAction.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = author.pubAction,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    val pubTime = author?.pubTimeText ?: author?.pubTime.orEmpty()
                    if (pubTime.isNotBlank()) {
                        Text(
                            text = pubTime,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Dynamic Text Content
            val content = item.contentText
            if (content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }

            // Video Archive Card (if AV / Video)
            val archive = item.videoArchive
            if (archive != null) {
                Spacer(modifier = Modifier.height(10.dp))
                DynamicVideoSection(archive = archive, onVideoClick = onVideoClick)
            }

            // Pictures (if Draw / Opus)
            val pics = item.pictureUrls
            if (pics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                DynamicPicturesSection(pictures = pics)
            }

            // Nested Forward Dynamic (if Repost / Forward)
            val orig = item.orig
            if (orig != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        val origAuthor = orig.modules?.moduleAuthor
                        if (origAuthor != null) {
                            Text(
                                text = "@${origAuthor.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiliPink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (orig.contentText.isNotBlank()) {
                            Text(
                                text = orig.contentText,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        orig.videoArchive?.let { origArchive ->
                            Spacer(modifier = Modifier.height(8.dp))
                            DynamicVideoSection(archive = origArchive, onVideoClick = onVideoClick)
                        }
                        val origPics = orig.pictureUrls
                        if (origPics.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DynamicPicturesSection(pictures = origPics)
                        }
                    }
                }
            }

            // Stat Footer (Forward, Comment, Like)
            Spacer(modifier = Modifier.height(12.dp))
            val stat = item.modules?.moduleStat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Forward
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "转发",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (stat?.forward?.count ?: 0).toString(),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Comment
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "评论",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (stat?.comment?.count ?: 0).toString(),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Like
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "点赞",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (stat?.like?.count ?: 0).toString(),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicVideoSection(
    archive: DynamicArchive,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                archive.bvid?.let { onVideoClick(it) }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = archive.cover,
                    contentDescription = archive.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Duration badge
                if (!archive.durationText.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = archive.durationText,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = archive.title ?: "视频",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    archive.stat?.play?.let { play ->
                        Text(
                            text = "播放 $play",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    archive.stat?.danmaku?.let { danmaku ->
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "弹幕 $danmaku",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicPicturesSection(
    pictures: List<String>,
    modifier: Modifier = Modifier
) {
    if (pictures.isEmpty()) return

    if (pictures.size == 1) {
        AsyncImage(
            model = pictures[0],
            contentDescription = "动态配图",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            pictures.take(3).forEach { picUrl ->
                AsyncImage(
                    model = picUrl,
                    contentDescription = "动态配图",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }
    }
}
