package com.example.mobilegomoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mobilegomoku.ui.theme.MobilegomokuTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.mobilegomoku.userdata.UserDatabase
import androidx.compose.runtime.LaunchedEffect
import com.example.mobilegomoku.userdata.UserEntity
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobilegomokuTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val userDao = UserDatabase.getInstance(context).userDao()
    val lastUserState = remember { mutableStateOf<UserEntity?>(null) }

    LaunchedEffect(Unit) {
        lastUserState.value = userDao.getLatestUser()
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lastUserState.value = userDao.getLatestUser()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val displayName = lastUserState.value?.username ?: "Guest"
    val (showSecondPlayerDialog, setShowSecondPlayerDialog) = remember { mutableStateOf(false) }
    var secondPlayerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome, $displayName",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Image(
            painter = painterResource(id = R.drawable.gomoku_logo),
            contentDescription = "Gomoku Logo",
            modifier = Modifier
                .size(300.dp)
                .padding(top = 32.dp),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, GameActivity::class.java)
                    intent.putExtra("playerSymbol", "X")
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp)
            ) {
                Text(
                    text = "Start Solo Game",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = {
                    val intent = Intent(context, GameActivity::class.java)
                    intent.putExtra("playerSymbol", "O")
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp)
            ) {
                Text(
                    text = "Play with a Friend (Bluetooth)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = {
                    setShowSecondPlayerDialog(true)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp)
            ) {
                Text(
                    text = "Play with a Friend on This Device",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = {
                    context.startActivity(Intent(context, StatsActivity::class.java))
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp)
            ) {
                Text(
                    text = "Show Stats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showSecondPlayerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { setShowSecondPlayerDialog(false) },
            title = { Text("Second Player") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = secondPlayerName,
                    onValueChange = { secondPlayerName = it },
                    label = { Text("Second Player Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(context, GameOneDeviceActivity::class.java)
                    intent.putExtra("playerSymbol", displayName)
                    intent.putExtra("opponentName", secondPlayerName)
                    context.startActivity(intent)
                    setShowSecondPlayerDialog(false)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { setShowSecondPlayerDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MobilegomokuTheme {
        MainScreen()
    }
}
