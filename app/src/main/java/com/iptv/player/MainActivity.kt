package com.iptv.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class Channel(
    val name: String,
    val url: String,
    val logo: String = "",
    val group: String = ""
)

val M3U_SOURCES = listOf(
    "https://iptv-org.github.io/iptv/categories/sports.m3u",
    "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IPTVApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPTVApp() {
    val BG = Color(0xFF0D0D1A)
    val CARD = Color(0xFF1A1A2E)
    val ACCENT = Color(0xFF00C9A7)
    val HEADER = Color(0xFF16213E)

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("All") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
                val all = mutableListOf<Channel>()
                for (src in M3U_SOURCES) {
                    try {
                        val req = Request.Builder().url(src)
                            .header("User-Agent", "IPTVPlayer/1.0")
                            .build()
                        val resp = client.newCall(req).execute()
                        val body = resp.body?.string() ?: continue
                        all.addAll(parseM3U(body))
                    } catch (_: Exception) {}
                }
                channels = all.distinctBy { it.url }
                isLoading = false
            } catch (e: Exception) {
                errorMsg = "লোড হয়নি: ${e.message}"
                isLoading = false
            }
        }
    }

    val groups = listOf("All") + channels
        .map { it.group.ifEmpty { "Other" } }
        .distinct()
        .sorted()

    val filtered = channels.filter { ch ->
        val matchSearch = searchQuery.isEmpty() ||
                ch.name.contains(searchQuery, ignoreCase = true)
        val matchTab = selectedTab == "All" ||
                ch.group.equals(selectedTab, ignoreCase = true) ||
                (selectedTab == "Other" && ch.group.isEmpty())
        matchSearch && matchTab
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "📺 IPTV Sports",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "${channels.size} channels",
                                fontSize = 11.sp,
                                color = ACCENT
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HEADER,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BG)
                    .padding(padding)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("🔍 Channel খুঁজুন...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CARD,
                        unfocusedContainerColor = CARD,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = ACCENT,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (groups.size > 1) {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups) { grp ->
                            FilterChip(
                                selected = selectedTab == grp,
                                onClick = { selectedTab = grp },
                                label = { Text(grp, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ACCENT,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CARD,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ACCENT, strokeWidth = 3.dp)
                                Spacer(Modifier.height(16.dp))
                                Text("Channels লোড হচ্ছে...", color = Color.Gray)
                            }
                        }
                    }
                    errorMsg.isNotEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("❌", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(errorMsg, color = Color(0xFFFF6B6B))
                            }
                        }
                    }
                    filtered.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("কোনো channel পাওয়া যায়নি", color = Color.Gray)
                        }
                    }
                    else -> {
                        Text(
                            "  ${filtered.size} channels",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filtered, key = { it.url }) { ch ->
                                ChannelCard(ch, CARD, ACCENT) {
                                    val intent = Intent(context, PlayerActivity::class.java).apply {
                                        putExtra("url", ch.url)
                                        putExtra("name", ch.name)
                                    }
                                    context.startActivity(intent)
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
fun ChannelCard(channel: Channel, cardColor: Color, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        channel.group.contains("football", ignoreCase = true) ||
                        channel.group.contains("soccer", ignoreCase = true) -> "⚽"
                        channel.group.contains("cricket", ignoreCase = true) -> "🏏"
                        channel.group.contains("basketball", ignoreCase = true) -> "🏀"
                        channel.group.contains("tennis", ignoreCase = true) -> "🎾"
                        channel.group.contains("motor", ignoreCase = true) ||
                        channel.group.contains("racing", ignoreCase = true) -> "🏎️"
                        else -> "📺"
                    },
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.group.isNotEmpty()) {
                    Text(
                        text = channel.group,
                        color = accent,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.Black, fontSize = 14.sp)
            }
        }
    }
}

fun parseM3U(content: String): List<Channel> {
    val channels = mutableListOf<Channel>()
    val lines = content.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("#EXTINF")) {
            val name = line.substringAfterLast(",").trim()
            val logo = Regex("""tvg-logo="([^"]*)"""").find(line)?.groupValues?.getOrNull(1) ?: ""
            val group = Regex("""group-title="([^"]*)"""").find(line)?.groupValues?.getOrNull(1) ?: ""
            val next = lines.getOrNull(i + 1)?.trim() ?: ""
            if (next.startsWith("http")) {
                if (name.isNotEmpty()) {
                    channels.add(Channel(name, next, logo, group))
                }
                i += 2
                continue
            }
        }
        i++
    }
    return channels
}
