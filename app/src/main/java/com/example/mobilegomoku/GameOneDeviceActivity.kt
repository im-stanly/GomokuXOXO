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
import kotlin.random.Random
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

enum class GamePhase {
    OPENING_MOVES,
    CHOICE,
    NORMAL_PLAY
}

fun getSymbolForOpeningMove(moveIndex: Int): String {
    return if (moveIndex < 2) "X" else "O"
}

class GameOneDeviceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val p1NameFromIntent = intent.getStringExtra("playerSymbol") ?: "Player1"
        val p2NameFromIntent = intent.getStringExtra("opponentName") ?: "Player2"
        setContent {
            MobilegomokuTheme {
                GameScreenOneDevice(
                    initialPlayer1Name = p1NameFromIntent,
                    initialPlayer2Name = p2NameFromIntent
                )
            }
        }
    }
}

@Composable
fun GameScreenOneDevice(
    initialPlayer1Name: String,
    initialPlayer2Name: String
) {
    val (openingPlayer, choicePlayer) = remember(initialPlayer1Name, initialPlayer2Name) {
        if (Random.nextBoolean()) {
            initialPlayer1Name to initialPlayer2Name
        } else {
            initialPlayer2Name to initialPlayer1Name
        }
    }

    var gamePhase by remember { mutableStateOf(GamePhase.OPENING_MOVES) }
    var openingMovesMade by remember { mutableStateOf(0) }

    var playerWhoIsX by remember { mutableStateOf("") }
    var playerWhoIsO by remember { mutableStateOf("") }
    var currentNormalPlaySymbol by remember { mutableStateOf("X") }

    var showChoiceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current as Activity
    val board = remember { List(14) { mutableStateListOf(*Array(14) { "" }) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$initialPlayer1Name vs $initialPlayer2Name",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Coin Toss: $openingPlayer makes opening moves (XXO).",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
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
                                        when (gamePhase) {
                                            GamePhase.OPENING_MOVES -> {
                                                if (openingMovesMade < 3) {
                                                    val symbolToPlace = getSymbolForOpeningMove(openingMovesMade)
                                                    board[row][col] = symbolToPlace
                                                    openingMovesMade++
                                                    if (openingMovesMade == 3) {
                                                        gamePhase = GamePhase.CHOICE
                                                        showChoiceDialog = true
                                                    }
                                                }
                                            }
                                            GamePhase.NORMAL_PLAY -> {
                                                board[row][col] = currentNormalPlaySymbol
                                                currentNormalPlaySymbol = if (currentNormalPlaySymbol == "X") "O" else "X"
                                            }
                                            GamePhase.CHOICE -> {
                                                // Board clicks disabled during choice phase
                                            }
                                        }
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
            Button(onClick = { context.finish() }) {
                Text("Back")
            }

            val turnText = when (gamePhase) {
                GamePhase.OPENING_MOVES -> {
                    if (openingMovesMade < 3) {
                        "Turn: $openingPlayer. Place ${getSymbolForOpeningMove(openingMovesMade)} (${openingMovesMade + 1}/3)"
                    } else {
                        "Opening moves complete. $choicePlayer to choose."
                    }
                }
                GamePhase.CHOICE -> "Turn: $choicePlayer. Choose your symbol."
                GamePhase.NORMAL_PLAY -> {
                    val currentPlayerName = if (currentNormalPlaySymbol == "X") playerWhoIsX else playerWhoIsO
                    "Turn: $currentPlayerName ($currentNormalPlaySymbol)"
                }
            }
            Text(
                modifier = Modifier.padding(end = 40.dp),
                text = turnText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showChoiceDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Symbol Choice for $choicePlayer") },
            text = { Text("$openingPlayer has made the opening moves (XXO). $choicePlayer, choose your symbol to continue.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        playerWhoIsX = choicePlayer
                        playerWhoIsO = openingPlayer
                        currentNormalPlaySymbol = "O"
                        gamePhase = GamePhase.NORMAL_PLAY
                        showChoiceDialog = false
                    }) {
                        Text("Play as X")
                    }
                    TextButton(onClick = {
                        playerWhoIsX = openingPlayer
                        playerWhoIsO = choicePlayer
                        currentNormalPlaySymbol = "X"
                        gamePhase = GamePhase.NORMAL_PLAY
                        showChoiceDialog = false
                    }) {
                        Text("Play as O")
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenOneDevicePreview() {
    MobilegomokuTheme {
        GameScreenOneDevice(initialPlayer1Name = "Alice", initialPlayer2Name = "Bob")
    }
}
