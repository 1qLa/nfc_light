package com.kotorin.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kotorin.app.ui.theme.*
import com.kotorin.app.ui.viewmodels.BluetoothViewModel
import com.kotorin.app.ui.viewmodels.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    onNavigateBack: () -> Unit,
    onConnectionSuccess: () -> Unit,
    viewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val devices by viewModel.foundDevices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var rememberDevice by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.startScan()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initBluetooth(context)
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onConnectionSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("デバイス選択", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "接続するデバイスを選択してください",
                fontSize = 14.sp,
                color = Gray600,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Scanning Status & List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Gray100, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                if (devices.isEmpty() && !isScanning) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Gray200,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "デバイスが見つかりません", color = Gray600)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(devices) { index, device ->
                            val isSelected = selectedIndex == index
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isSelected) Emerald500.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedIndex = index }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isSelected) Emerald500 else Gray200,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = device.name ?: "Unknown Device",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        if (device.isDemo) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "デモ",
                                                fontSize = 10.sp,
                                                color = Emerald500,
                                                modifier = Modifier
                                                    .background(
                                                        Emerald500.copy(alpha = 0.1f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(text = device.id, fontSize = 12.sp, color = Gray600)
                                    Text(text = "${device.rssi} dBm", fontSize = 11.sp, color = Gray200)
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Emerald500
                                    )
                                }
                            }
                        }
                    }
                }

                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp),
                        color = Emerald500,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberDevice,
                    onCheckedChange = { rememberDevice = it },
                    colors = CheckboxDefaults.colors(checkedColor = Emerald500)
                )
                Text(text = "このデバイスを記憶する", fontSize = 14.sp, color = Gray600)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            OutlinedButton(
                onClick = { viewModel.loadDemoDevices() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald500),
                border = ButtonDefaults.outlinedButtonBorder,
                enabled = !isScanning
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "デモデータを表示", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    permissionLauncher.launch(permissions)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gray100),
                enabled = !isScanning
            ) {
                Text(text = if (isScanning) "スキャン中..." else "デバイスを再検索", color = Gray600)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    selectedIndex?.let { viewModel.connect(devices[it]) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                enabled = selectedIndex != null && connectionState in listOf(
                    ConnectionState.NONE, ConnectionState.DISCONNECTED, ConnectionState.ERROR
                )
            ) {
                if (connectionState == ConnectionState.CONNECTING) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(text = "接続する", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
