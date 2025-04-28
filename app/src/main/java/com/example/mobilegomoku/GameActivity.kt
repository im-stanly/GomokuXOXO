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
                .padding(top = 32.dp, bottom = 24.dp)
        )
        // Placeholder for the game board
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Game Board Placeholder",
                fontSize = 18.sp,
                color = androidx.compose.ui.graphics.Color.Gray
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
