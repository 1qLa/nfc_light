package com.kotorin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kotorin.app.ui.screens.*
import com.kotorin.app.ui.theme.KotorinAppTheme
import com.kotorin.app.ui.viewmodels.BluetoothViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KotorinAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // BluetoothViewModel をここで生成し全画面で共有
    val bluetoothViewModel: BluetoothViewModel = viewModel()

    NavHost(navController = navController, startDestination = "bluetooth") {

        // ① デバイス選択
        composable("bluetooth") {
            BluetoothScreen(
                viewModel = bluetoothViewModel,
                onNavigateBack = { navController.popBackStack() },
                onConnectionSuccess = {
                    navController.navigate("bluetooth_connecting") {
                        popUpTo("bluetooth") { inclusive = false }
                    }
                }
            )
        }

        // ② 接続完了 → 新規登録 or ログイン選択
        composable("bluetooth_connecting") {
            BluetoothConnectingScreen(
                viewModel = bluetoothViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = { navController.navigate("register_password") },
                onNavigateToLogin = { navController.navigate("login") }
            )
        }

        // ③ 新規登録：パスワード入力
        composable("register_password") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNext = { navController.navigate("security_question") }
            )
        }

        // ④ 秘密の質問：登録
        composable("security_question") {
            SecurityQuestionScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterComplete = {
                    navController.navigate("login") {
                        popUpTo("bluetooth_connecting") { inclusive = false }
                    }
                }
            )
        }

        // ⑥ ログイン
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register_password") },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("bluetooth_connecting") { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate("password_reset") }
            )
        }

        // ⑦ パスワードリセット
        composable("password_reset") {
            PasswordResetScreen(
                onNavigateBack = { navController.popBackStack() },
                onNext = { navController.navigate("password_reconfirm") }
            )
        }

        // ⑧ パスワード再確認
        composable("password_reconfirm") {
            PasswordReconfirmScreen(
                onNavigateBack = { navController.popBackStack() },
                onResetComplete = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // ⑨ デバイスホーム：ライト操作
        composable("home") {
            HomeScreen(
                viewModel = bluetoothViewModel,
                onNavigateToKeyManagement = { navController.navigate("device_settings") },
                onNavigateToSecurityQuestion = { navController.navigate("security_question_edit") },
                onDisconnect = {
                    navController.navigate("bluetooth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ⑩ 鍵の管理
        composable("device_settings") {
            DeviceSettingsScreen(
                viewModel = bluetoothViewModel,
                onNavigateBack = { navController.popBackStack() },
                onDisconnect = { }
            )
        }

        // ⑪ 秘密の質問を編集
        composable("security_question_edit") {
            SecurityQuestionEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
