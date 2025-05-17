package com.example.mobilegomoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mobilegomoku.ui.theme.MobilegomokuTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.example.mobilegomoku.userdata.UserDatabase
import com.example.mobilegomoku.userdata.UserEntity

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
    val context = LocalContext.current
    val userDao = UserDatabase.getInstance(context).userDao()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loggedInUser by remember { mutableStateOf<String?>(null) }

    var showSignUpDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    // Track login errors
    var loginError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { showSignUpDialog = true },
                modifier = Modifier.width(100.dp)
            ) {
                Text("Sign Up", fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = { showLoginDialog = true },
                modifier = Modifier.width(100.dp)
            ) {
                Text("Log In", fontSize = 14.sp)
            }
        }

        // Display logged-in user or “Guest”
        Text(
            text = if (loggedInUser.isNullOrEmpty()) "Guest" else "Logged in as $loggedInUser",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Additional row for logout & delete account if user is logged in
        if (!loggedInUser.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        // Log out
                        loggedInUser = null
                    }
                ) {
                    Text("Log Out")
                }
                Button(
                    onClick = {
                        // Delete account
                        userDao.deleteUser(loggedInUser!!)
                        loggedInUser = null
                    }
                ) {
                    Text("Delete Account")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Player Statistics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Win streak: x",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Win count: x",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Loose count: x",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(55.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Back to Main Screen", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Sign Up dialog
        if (showSignUpDialog) {
            AlertDialog(
                onDismissRequest = { showSignUpDialog = false },
                title = { Text("Sign Up") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Store in DB
                        userDao.insertUser(UserEntity(username, password))
                        loggedInUser = username
                        showSignUpDialog = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignUpDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Log In dialog
        if (showLoginDialog) {
            AlertDialog(
                onDismissRequest = {
                    showLoginDialog = false
                    loginError = "" // clear error when dialog closes
                },
                title = { Text("Log In") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Show login error if any
                        if (loginError.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = loginError,
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val user = userDao.getUser(username)
                        if (user != null && user.password == password) {
                            loggedInUser = user.username
                            showLoginDialog = false
                            loginError = "" // clear error on success
                        } else {
                            loginError = "Invalid username or password"
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showLoginDialog = false
                        loginError = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
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

