package com.kotorin.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotorin.app.ui.theme.*
import com.kotorin.app.ui.viewmodels.BluetoothViewModel
import com.kotorin.app.ui.viewmodels.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BluetoothViewModel,
    onNavigateToKeyManagement: () -> Unit,
    onNavigateToSecurityQuestion: () -> Unit,
    onDisconnect: () -> Unit
) {
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var lightOn by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("切断の確認") },
            text = { Text("タグとの接続を切断しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectDialog = false
                    viewModel.disconnect()
                    onDisconnect()
                }) {
                    Text("切断する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("キャンセル", color = Gray600)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = connectedDevice?.name ?: "ESP32-Security-01",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gray800
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (connectionState == ConnectionState.CONNECTED) Emerald500 else Gray200,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (connectionState == ConnectionState.CONNECTED) "接続中" else "切断中",
                                    fontSize = 12.sp,
                                    color = if (connectionState == ConnectionState.CONNECTED) Emerald500 else Gray600
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Gray100
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ライトカード（タップでON/OFF切替）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable {
                        lightOn = !lightOn
                        val cmd = if (lightOn) "LED_ON" else "LED_OFF"
                        viewModel.sendData(cmd.toByteArray())
                    }
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (lightOn) Icons.Default.LightMode else Icons.Default.NightlightRound,
                        contentDescription = "ライト切替",
                        tint = if (lightOn) Color(0xFFFBBF24) else Gray200,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (lightOn) "ライト ON" else "ライト OFF",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lightOn) Gray800 else Gray600
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ライト状態バッジ行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ライト状態", fontSize = 14.sp, color = Gray800, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (lightOn) Emerald500.copy(alpha = 0.12f) else Gray200,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (lightOn) "点灯中" else "消灯中",
                        fontSize = 12.sp,
                        color = if (lightOn) Emerald500 else Gray600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 認証ステータスボタン
            Button(
                onClick = { viewModel.sendData("STATUS".toByteArray()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) {
                Text(
                    "認証済み - ライトが使用可能です",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 鍵の管理 / 秘密の質問
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToKeyManagement,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gray800)
                ) {
                    Text("🔑 鍵の管理", color = Color.White, fontSize = 14.sp)
                }
                Button(
                    onClick = onNavigateToSecurityQuestion,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gray800)
                ) {
                    Text("❓ 秘密の質問", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // タグを切断
            OutlinedButton(
                onClick = { showDisconnectDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = Color.White
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("タグを切断", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
