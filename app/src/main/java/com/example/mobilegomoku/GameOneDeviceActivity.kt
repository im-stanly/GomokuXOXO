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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height

class GameOneDeviceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playerSymbol = intent.getStringExtra("playerSymbol") ?: "Player1"
        val opponentName = intent.getStringExtra("opponentName") ?: "Player2"
        setContent {
            MobilegomokuTheme {
                GameScreenOneDevice(playerSymbol = playerSymbol, opponentName = opponentName)
            }
        }
    }
}

@Composable
fun GameScreenOneDevice(
    playerSymbol: String,
    opponentName: String
) {
    var currentPlayerTurn by remember { mutableStateOf(playerSymbol) }
    val context = LocalContext.current as Activity

    val board = remember {
        List(14) {
            mutableStateListOf(*Array(14) { "" })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$playerSymbol vs $opponentName",
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
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                for (row in 0 until 14) {
                    Row {
                        for (col in 0 until 14) {
                            Button(
                                onClick = {
                                    if (board[row][col].isEmpty()) {
                                        board[row][col] = currentPlayerTurn
                                        currentPlayerTurn = if (currentPlayerTurn == "X") "O" else "X"
                                    }
                                },
                                modifier = Modifier
                                    .padding(0.1.dp)
                                    .width(60.dp)
                                    .height(45.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFebae34)
                                )
                            ) {
                                Text(
                                    text = board[row][col],
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Left
                                )
                            }
                        }
                    }
                }
            }
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
fun GameScreenOneDevicePreview() {
    MobilegomokuTheme {
        GameScreen(playerSymbol = "O")
    }
}
