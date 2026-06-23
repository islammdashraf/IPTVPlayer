package com.iptv.player

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val url = intent.getStringExtra("url") ?: ""
        val name = intent.getStringExtra("name") ?: "Live TV"
        setContent { PlayerScreen(url = url, name = name) }
    }
}

@Composable
fun PlayerScreen(url: String, name: String) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    hasError = state == Player.STATE_IDLE
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isBuffering) Color.Yellow else Color(0xFF00C9A7),
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    if (isBuffering) "Buffering..." else "🔴 LIVE",
                    color = if (isBuffering) Color.Yellow else Color(0xFF00C9A7),
                    fontSize = 12.sp
                )
            }
        }

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF00C9A7),
                strokeWidth = 3.dp
            )
        }

        if (hasError) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("❌", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("Stream চালু হয়নি", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Channel unavailable", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
