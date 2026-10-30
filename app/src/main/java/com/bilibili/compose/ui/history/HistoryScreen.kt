package com.bilibili.compose.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bilibili.compose.data.model.HistoryEntry
import com.bilibili.compose.ui.theme.BiliPink
import com.bilibili.compose.ui.theme.TextPrimary
import com.bilibili.compose.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tabs = listOf("综合" to "all", "视频" to "archive", "直播" to "live", "专栏" to "article")
    val filtered = remember(state.entries, state.searchQuery, state.timeFilter) {
        val now = System.currentTimeMillis() / 1000
        val today = startOfDay(now)
        val start = when (state.timeFilter) {
            "今天" -> today
            "昨天" -> today - 86400
            "近一周" -> today - 7 * 86400
            else -> 0L
        }
        state.entries.filter { entry ->
            val matchesText = state.searchQuery.isBlank() ||
                entry.title.contains(state.searchQuery, ignoreCase = true) ||
                entry.authorName.contains(state.searchQuery, ignoreCase = true)
            val matchesTime = when (state.timeFilter) {
                "昨天" -> entry.viewAt in start until (start + 86400)
                "今天", "近一周" -> entry.viewAt >= start
                else -> true
            }
            matchesText && matchesTime
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("历史记录", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.second == state.selectedType }.coerceAtLeast(0)) {
                tabs.forEach { (label, type) ->
                    Tab(selected = state.selectedType == type, onClick = { viewModel.setType(type) }, text = { Text(label) })
                }
            }
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("搜索标题 / UP主昵称") }, singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("全部时间", "今天", "昨天", "近一周").forEach { filter ->
                    FilterChip(selected = state.timeFilter == filter, onClick = { viewModel.setTimeFilter(filter) }, label = { Text(filter, fontSize = 12.sp) })
                }
            }
            when {
                state.isLoading && state.entries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BiliPink)
                }
                state.errorMessage != null && state.entries.isEmpty() -> Column(
                    Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.errorMessage ?: "加载失败", color = TextSecondary)
                    TextButton(onClick = { viewModel.refresh() }) { Text("重试", color = BiliPink) }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(if (state.entries.isEmpty()) "暂无历史记录" else "没有找到相关记录", color = TextSecondary)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filtered.groupBy { dayLabel(it.viewAt) }.forEach { (day, entries) ->
                        item(key = "header-$day") {
                            Text(day, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                        }
                        items(entries, key = { if (it.kid != 0L) it.kid.toString() else "${it.history.bvid}-${it.viewAt}" }) { entry ->
                            HistoryRow(entry) { if (entry.history.bvid.isNotBlank()) onVideoClick(entry.history.bvid) }
                        }
                    }
                    item {
                        if (state.hasMore) {
                            TextButton(onClick = { viewModel.loadMore() }, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoadingMore) {
                                if (state.isLoadingMore) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("加载更多", color = BiliPink)
                            }
                        } else {
                            Text("已经到底了", color = TextSecondary, fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val duration = entry.duration.coerceAtLeast(0)
    val progress = entry.progress.coerceAtLeast(0)
    val fraction = if (duration > 0 && entry.progress >= 0) (progress.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(9.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(model = entry.cover, contentDescription = entry.title,
                modifier = Modifier.width(132.dp).height(82.dp).clip(RoundedCornerShape(7.dp)), contentScale = ContentScale.Crop)
            if (duration > 0) Text(formatDuration(duration), color = Color.White, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Text(entry.authorName, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
            Text(formatViewTime(entry.viewAt), fontSize = 10.sp, color = TextSecondary)
            if (duration > 0 && entry.progress >= 0) {
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(3.dp), color = BiliPink)
            } else if (entry.progress < 0) Text("已看完", fontSize = 10.sp, color = TextSecondary)
        }
    }
}

private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timestamp * 1000
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis / 1000
private fun dayLabel(timestamp: Long): String {
    val today = startOfDay(System.currentTimeMillis() / 1000)
    val recordDay = startOfDay(timestamp)
    return when {
        recordDay == today -> "今天"
        recordDay == today - 86400 -> "昨天"
        recordDay >= today - 6 * 86400 -> "近一周"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp * 1000))
    }
}
private fun formatViewTime(timestamp: Long): String = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp * 1000))
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs) else "%02d:%02d".format(minutes, secs)
}