package com.example.mobilegomoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.mobilegomoku.ui.theme.MobilegomokuTheme
import android.app.Activity
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobilegomoku.userdata.UserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import androidx.compose.material3.TextButton

enum class WifiGamePhase {
    COIN_TOSS_PENDING,
    LOCAL_PLAYER_OPENING,
    AWAITING_API_CHOICE,
    AWAITING_API_OPENING,
    NORMAL_PLAY,
    GAME_OVER
}

private const val BASE_URL = "http://<IP>:8080" //ip to komenda "ipconfig getifaddr en0" oraz port 8080

class GameWithWifiActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actualHostPlayerName = intent.getStringExtra("loggedInUserName") ?: "Guest"

        mediaPlayer = MediaPlayer.create(this, R.raw.applause)

        setContent {
            MobilegomokuTheme {
                GameWithWifiScreen(
                    hostPlayerName = actualHostPlayerName,
                    onGameEnd = {
                        mediaPlayer?.start()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}

@Composable
fun GameWithWifiScreen(
    hostPlayerName: String,
    onGameEnd: () -> Unit
) {
    val context = LocalContext.current as Activity
    val userDao = UserDatabase.getInstance(context).userDao()
    val coroutineScope = rememberCoroutineScope()

    val board = remember { List(14) { mutableStateListOf(*Array(14) { "" }) } }
    var gamePhase by remember { mutableStateOf(WifiGamePhase.COIN_TOSS_PENDING) }

    var localPlayerSymbol by remember { mutableStateOf("X") }
    var opponentApiSymbol by remember { mutableStateOf("O") }
    var currentTurnSymbol by remember { mutableStateOf("X") }

    var opponentApiName by remember { mutableStateOf("Opponent API") }

    var winnerName by remember { mutableStateOf<String?>(null) }
    var winningCoords by remember { mutableStateOf(emptyList<Pair<Int, Int>>()) }

    var showCoinTossDialog by remember { mutableStateOf(true) }
    var showSymbolChoiceDialog by remember { mutableStateOf(false) }
    var localPlayerOpeningMovesCount by remember { mutableStateOf(0) }

    suspend fun fetchApiOpeningMoves(): List<Triple<Int, Int, String>> {
        return realFetchFirstMove().second
    }

    suspend fun fetchApiSymbolChoice(currentBoard: List<List<String>>): String {
        return realFetchSymbolChoice(currentBoard).second
    }

    suspend fun fetchApiNextMove(currentBoard: List<List<String>>): Pair<Int, Int> {
        return realMakeOpponentMove(currentBoard)
    }

    fun applyMovesToBoard(moves: List<Triple<Int, Int, String>>) {
        moves.forEach { (r, c, sym) ->
            if (r in board.indices && c in board[r].indices) {
                board[r][c] = sym
            }
        }
    }

    fun makeApiMove(isOpening: Boolean = false) {
        coroutineScope.launch {
            try {
                val (r, c) = fetchApiNextMove(board.map { it.toList() })
                if (board[r][c].isEmpty()) {
                    board[r][c] = opponentApiSymbol
                    val win = checkWinWifi(board, opponentApiSymbol, r, c)
                    if (win != null) {
                        winningCoords = win
                        winnerName = opponentApiName
                        gamePhase = WifiGamePhase.GAME_OVER
                        onGameEnd()

                        if (hostPlayerName != "Guest") {
                            coroutineScope.launch {
                                userDao.updateResult(hostPlayerName, 0)
                            }
                        }
                    } else {
                        currentTurnSymbol = localPlayerSymbol
                        if (isOpening) gamePhase = WifiGamePhase.NORMAL_PLAY
                    }
                } else {
                    currentTurnSymbol = localPlayerSymbol
                    Toast.makeText(context, "API tried an invalid move. Your turn.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error getting API's next move: ${e.message}", Toast.LENGTH_LONG).show()
                currentTurnSymbol = localPlayerSymbol
            }
        }
    }

    if (showCoinTossDialog) {
        AlertDialog(
            onDismissRequest = { /* Must make a choice */ },
            title = { Text("Coin Toss") },
            text = { Text("Who starts the opening moves?") },
            confirmButton = {
                OutlinedButton(onClick = {
                    showCoinTossDialog = false
                    gamePhase = WifiGamePhase.LOCAL_PLAYER_OPENING
                    currentTurnSymbol = "X"
                    localPlayerSymbol = "X"
                    opponentApiSymbol = "O"
                    Toast.makeText(context, "$hostPlayerName starts opening.", Toast.LENGTH_SHORT).show()
                }) { Text(hostPlayerName) }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showCoinTossDialog = false
                    gamePhase = WifiGamePhase.AWAITING_API_OPENING
                    Toast.makeText(context, "$opponentApiName starts opening.", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        try {
                            val (apiName, openingMoves) = realFetchFirstMove()
                            opponentApiName = apiName
                            applyMovesToBoard(openingMoves)
                            showSymbolChoiceDialog = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error fetching API opening moves: ${e.message}", Toast.LENGTH_LONG).show()
                            context.finish()
                        }
                    }
                }) { Text(opponentApiName) }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$hostPlayerName ($localPlayerSymbol) vs $opponentApiName ($opponentApiSymbol)",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = when (gamePhase) {
                WifiGamePhase.COIN_TOSS_PENDING -> "Deciding who starts..."
                WifiGamePhase.LOCAL_PLAYER_OPENING -> {
                    val moveNumber = localPlayerOpeningMovesCount + 1
                    val symbolToPlace = if (localPlayerOpeningMovesCount < 2) "X" else "O"
                    "$hostPlayerName's opening move $moveNumber/3 (Place $symbolToPlace)"
                }
                WifiGamePhase.AWAITING_API_CHOICE -> "Waiting for $opponentApiName to choose symbol..."
                WifiGamePhase.AWAITING_API_OPENING -> "Waiting for $opponentApiName's opening moves..."
                WifiGamePhase.NORMAL_PLAY -> {
                    val currentPlayerName = if (currentTurnSymbol == localPlayerSymbol) hostPlayerName else opponentApiName
                    "Turn: $currentPlayerName ($currentTurnSymbol)"
                }
                WifiGamePhase.GAME_OVER -> "$winnerName wins!"
            },
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (showSymbolChoiceDialog && gamePhase == WifiGamePhase.AWAITING_API_OPENING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    coroutineScope.launch {
                        realAnnounceSymbolChoice("X")
                        localPlayerSymbol = "X"
                        opponentApiSymbol = "O"
                        currentTurnSymbol = opponentApiSymbol
                        showSymbolChoiceDialog = false
                        gamePhase = WifiGamePhase.NORMAL_PLAY
                        makeApiMove()
                    }
                }) {
                    Text("Play as X")
                }
                Button(onClick = {
                    coroutineScope.launch {
                        realAnnounceSymbolChoice("O")
                        localPlayerSymbol = "O"
                        opponentApiSymbol = "X"
                        currentTurnSymbol = opponentApiSymbol
                        showSymbolChoiceDialog = false
                        gamePhase = WifiGamePhase.NORMAL_PLAY
                        makeApiMove()
                    }
                }) {
                    Text("Play as O")
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column {
                for (r in 0 until 14) {
                    Row {
                        for (c in 0 until 14) {
                            Button(
                                onClick = {
                                    if (gamePhase == WifiGamePhase.GAME_OVER || board[r][c].isNotEmpty()) return@Button

                                    if (gamePhase == WifiGamePhase.LOCAL_PLAYER_OPENING) {
                                        //board[1][1] = "O"
                                        val symbolToPlace = if (localPlayerOpeningMovesCount < 2) "X" else "O"
                                        board[r][c] = symbolToPlace
                                        localPlayerOpeningMovesCount++
                                        if (localPlayerOpeningMovesCount == 3) {
                                            gamePhase = WifiGamePhase.AWAITING_API_CHOICE
                                            coroutineScope.launch {
                                                try {
                                                    val apiChosenSymbol = fetchApiSymbolChoice(board.map { it.toList() })
                                                    opponentApiSymbol = apiChosenSymbol
                                                    localPlayerSymbol = if (apiChosenSymbol == "X") "O" else "X"

                                                    currentTurnSymbol = localPlayerSymbol
                                                    gamePhase = WifiGamePhase.NORMAL_PLAY
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "Error getting API symbol choice: ${e.message}", Toast.LENGTH_LONG).show()
                                                    gamePhase = WifiGamePhase.LOCAL_PLAYER_OPENING
                                                    localPlayerOpeningMovesCount = 0
                                                    board.forEach { row -> row.fill("") }
                                                }
                                            }
                                        }
                                    } else if (gamePhase == WifiGamePhase.NORMAL_PLAY && currentTurnSymbol == localPlayerSymbol) {
                                        board[r][c] = localPlayerSymbol
                                        val win = checkWinWifi(board, localPlayerSymbol, r, c)
                                        if (win != null) {
                                            winningCoords = win
                                            winnerName = hostPlayerName
                                            gamePhase = WifiGamePhase.GAME_OVER
                                            onGameEnd()
                                            if (hostPlayerName != "Guest") {
                                                coroutineScope.launch {
                                                    userDao.updateResult(hostPlayerName, 1)
                                                }
                                            }
                                        } else {
                                            currentTurnSymbol = opponentApiSymbol
                                            makeApiMove()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .padding(0.1.dp)
                                    .width(60.dp)
                                    .height(45.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (winningCoords.contains(r to c)) Color.Green else Color(0xFFebae34)
                                )
                            ) {
                                Text(text = board[r][c], fontSize = 20.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { context.finish() }) { Text("Back") }
        }
    }
}

private val client = OkHttpClient()

private suspend fun realFetchFirstMove(): Pair<String, List<Triple<Int, Int, String>>> = withContext(Dispatchers.IO) {
    val url = "$BASE_URL/game/first-move"
    val request = Request.Builder().url(url).get().build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Network error (${response.code}): ${response.message}")
        val bodyStr = response.body?.string() ?: throw Exception("Empty response body from /first-move")
        val json = JSONObject(bodyStr)
        val apiOpponentName = json.optString("opponentName", "Opponent API")
        val movesJsonArray = json.getJSONArray("moves")
        val movesList = mutableListOf<Triple<Int, Int, String>>()
        for (i in 0 until movesJsonArray.length()) {
            val moveObj = movesJsonArray.getJSONObject(i)
            movesList.add(Triple(moveObj.getInt("row"), moveObj.getInt("col"), moveObj.getString("symbol")))
        }
        apiOpponentName to movesList
    }
}

private suspend fun realFetchSymbolChoice(boardData: List<List<String>>): Pair<String, String> = withContext(Dispatchers.IO) {
    val url = "$BASE_URL/game/choose"
    val boardJsonArray = JSONArray()
    boardData.forEach { rowList ->
        val rowJsonArray = JSONArray()
        rowList.forEach { cell -> rowJsonArray.put(cell) }
        boardJsonArray.put(rowJsonArray)
    }
    val requestJson = JSONObject().put("board", boardJsonArray)
    val requestBody: RequestBody = requestJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder().url(url).post(requestBody).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Network error (${response.code}) from /choose: ${response.message}")
        val bodyStr = response.body?.string() ?: throw Exception("Empty response body from /choose")
        val obj = JSONObject(bodyStr)
        val apiChosenSymbol = obj.getString("chosenSymbol")
        val hostPlayerSym = if (apiChosenSymbol == "X") "O" else "X"
        hostPlayerSym to apiChosenSymbol
    }
}

private suspend fun realAnnounceSymbolChoice(chosenSymbol: String) = withContext(Dispatchers.IO) {
    val url = "$BASE_URL/game/announce-symbol"
    val json = JSONObject().put("chosenSymbol", chosenSymbol)
    val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder().url(url).post(body).build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Network error ${response.code}")
    }
}

private suspend fun realMakeOpponentMove(boardData: List<List<String>>): Pair<Int, Int> = withContext(Dispatchers.IO) {
    val url = "$BASE_URL/game/next-move"
    val boardJsonArray = JSONArray()
    boardData.forEach { rowList ->
        val rowJsonArray = JSONArray()
        rowList.forEach { cell -> rowJsonArray.put(cell) }
        boardJsonArray.put(rowJsonArray)
    }
    val requestJson = JSONObject().put("board", boardJsonArray)
    val requestBody: RequestBody = requestJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
    val request = Request.Builder().url(url).post(requestBody).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw Exception("Network error (${response.code}) from /next-move: ${response.message}")
        val bodyStr = response.body?.string() ?: throw Exception("Empty response body from /next-move")
        val obj = JSONObject(bodyStr)
        obj.getInt("row") to obj.getInt("col")
    }
}

fun checkWinWifi(board: List<MutableList<String>>, symbol: String, r: Int, c: Int): List<Pair<Int, Int>>? {
    if (symbol.isEmpty()) return null
    val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
    val boardSize = board.size

    for ((dr, dc) in directions) {
        var count = 1
        val lineCoords = mutableListOf(r to c)

        for (i in 1 until 5) {
            val nr = r + dr * i
            val nc = c + dc * i
            if (nr in 0 until boardSize && nc in 0 until boardSize && board[nr][nc] == symbol) {
                count++
                lineCoords.add(nr to nc)
            } else break
        }

        for (i in 1 until 5) {
            val nr = r - dr * i
            val nc = c - dc * i
            if (nr in 0 until boardSize && nc in 0 until boardSize && board[nr][nc] == symbol) {
                count++
                lineCoords.add(nr to nc)
            } else break
        }
        if (count >= 5) return lineCoords
    }
    return null
}
