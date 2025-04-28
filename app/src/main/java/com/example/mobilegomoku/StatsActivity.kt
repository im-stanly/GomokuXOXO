package com.example.mobilegomoku

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mobilegomoku.ui.theme.MobilegomokuTheme

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobilegomokuTheme {
                StatsScreen(onBackClick = { finish() })
            }
        }
    }
}

@Composable
fun StatsScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Win streak: x", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Win count: x", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Loose count: x", modifier = Modifier.padding(bottom = 32.dp))
        Button(onClick = onBackClick) {
            Text(text = "Back to Main Screen")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    MobilegomokuTheme {
        StatsScreen(onBackClick = {})
    }
}
