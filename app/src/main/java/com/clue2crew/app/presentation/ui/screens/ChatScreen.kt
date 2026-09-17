package com.clue2crew.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clue2crew.app.data.local.entities.MessageEntity
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(navController: NavController, viewModel: FamilyViewModel) {
    val messages by viewModel.messages.collectAsState()
    val family by viewModel.family.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Clue2CrewScaffold(navController = navController, viewModel = viewModel) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1B263B),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Family Chat",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = family?.familyName ?: "Offline Group",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }

            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0D1B2A),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF415A77),
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .background(Color(0xFF415A77), RoundedCornerShape(24.dp))
                            .size(48.dp),
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
    ) {
        if (!message.isMe) {
            Text(
                text = message.senderName,
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        
        Surface(
            color = if (message.isMe) Color(0xFF415A77) else Color(0xFF1B263B),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMe) 16.dp else 0.dp,
                bottomEnd = if (message.isMe) 0.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = timeString,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
