package com.example.mobilegomoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilegomoku.ui.theme.MobilegomokuTheme
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playerSymbol = intent.getStringExtra("playerSymbol") ?: run {
            android.util.Log.e("GameActivity", "Missing playerSymbol in Intent")
            finish()
            return
        }
        setContent {
            MobilegomokuTheme {
                GameScreen(playerSymbol = playerSymbol)
            }
        }
    }
}

@Composable
fun GameScreen(playerSymbol: String = "X") {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var currentPlayerTurn by remember { mutableStateOf(playerSymbol) }
    val minScale = 1f
    val maxScale = 5f
    val doubleTapZoomScale = 2f
    val context = LocalContext.current as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "You play with $playerSymbol",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 32.dp, bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.board),
                contentDescription = "Game Board",
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (scale > minScale) {
                                    scale = minScale
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    val newScale = doubleTapZoomScale.coerceIn(minScale, maxScale)
                                    offsetX = tapOffset.x * (1 - newScale / scale) + offsetX * (newScale / scale)
                                    offsetY = tapOffset.y * (1 - newScale / scale) + offsetY * (newScale / scale)
                                    scale = newScale
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(minScale, maxScale)

                            offsetX = (offsetX - centroid.x) * (newScale / oldScale) + centroid.x + pan.x
                            offsetY = (offsetY - centroid.y) * (newScale / oldScale) + centroid.y + pan.y

                            scale = newScale
                        }
                    }
                    .fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                context.finish()
            }) {
                Text("Back")
            }
            Text(
                modifier = Modifier.padding(end = 40.dp),
                text = "Turn: $currentPlayerTurn",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    MobilegomokuTheme {
        GameScreen(playerSymbol = "O")
    }
}
