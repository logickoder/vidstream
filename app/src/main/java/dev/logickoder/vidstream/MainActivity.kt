package dev.logickoder.vidstream

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.logickoder.vidstream.domain.UserRole
import dev.logickoder.vidstream.ui.theme.VidstreamTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VidstreamTheme {
                JoinCallScreen()
            }
        }
    }

    @Composable
    private fun JoinCallScreen(
        modifier: Modifier = Modifier,
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            content = { scaffoldPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = {
                        Text(
                            text = "Live Video Streaming",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(50.dp))
                        InputFields(modifier)
                    }
                )
            }
        )
    }

    @Composable
    private fun InputFields(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        var channelName by remember { mutableStateOf("test") }
        var selectedRole by remember { mutableStateOf(UserRole.Broadcaster) }

        Column(
            modifier = modifier.padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.Center,
            content = {
                Spacer(modifier = modifier.height(80.dp))

                TextField(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .fillMaxWidth(),
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Channel Name ") },
                    placeholder = { Text("test") },
                    readOnly = true
                )

//                Spacer(modifier = modifier.height(16.dp))
//
//                UserRole.entries.forEach { role ->
//                    Row(
//                        Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 10.dp)
//                            .selectable(
//                                selected = (role == selectedRole),
//                                onClick = { selectedRole = role }
//                            ),
//                        content = {
//                            RadioButton(
//                                selected = role == selectedRole,
//                                modifier = modifier.padding(
//                                    horizontal = 25.dp,
//                                    vertical = 0.dp
//                                ),
//                                onClick = {
//                                    selectedRole = role
//                                }
//                            )
//                            Text(
//                                text = role.name,
//                                modifier = Modifier.padding(start = 10.dp, top = 10.dp),
//                                fontSize = 18.sp
//                            )
//                        }
//                    )
//                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (channelName.isNotEmpty()) {
                            val intent = Intent(context, VideoCallActivity::class.java)
                            intent.putExtra("channel_name", channelName)
                            intent.putExtra("role", selectedRole.name)
                            startActivity(intent)
                        }
                    },
                    contentPadding = PaddingValues(
                        horizontal = 20.dp,
                        vertical = 10.dp
                    ),
                    enabled = channelName.isNotEmpty(),
                    content = {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Join",
                            modifier = modifier.size(24.dp)
                        )
                        Spacer(modifier = modifier.size(ButtonDefaults.IconSpacing))
                        Text(text = "Join", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                )
            }
        )
    }
}
