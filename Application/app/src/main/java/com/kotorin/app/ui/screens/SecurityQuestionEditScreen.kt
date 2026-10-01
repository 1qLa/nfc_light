package com.kotorin.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotorin.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionEditScreen(
    onNavigateBack: () -> Unit
) {
    val questions = listOf(
        "最初に飼ったペットの名前は？",
        "母親の旧姓は？",
        "小学校の名前は？",
        "生まれた病院の名前は？",
        "子供の頃の一番好きな食べ物は？"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedQuestion by remember { mutableStateOf(questions[0]) }
    var answer by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "戻る")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "秘密の質問を編集",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Gray800
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "パスワードを忘れた時に使用します",
                fontSize = 14.sp,
                color = Gray600
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 現在の秘密の質問（表示のみ）
            Text("現在の秘密の質問", fontSize = 13.sp, color = Gray600)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "最初に飼ったペットの名前は？",
                fontSize = 15.sp,
                color = Gray800
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 新しい秘密の質問ドロップダウン
            Text("新しい秘密の質問", fontSize = 13.sp, color = Gray600)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedQuestion,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Gray600)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = Gray200,
                        focusedBorderColor = Emerald500
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    questions.forEach { question ->
                        DropdownMenuItem(
                            text = { Text(question, fontSize = 14.sp) },
                            onClick = {
                                selectedQuestion = question
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 新しい回答
            Text("新しい回答", fontSize = 13.sp, color = Gray600)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                placeholder = { Text("質問の回答を入力", color = Gray600) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    unfocusedBorderColor = Gray200,
                    focusedBorderColor = Emerald500
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "※ 変更はデバイスに書き込まれます",
                fontSize = 12.sp,
                color = Gray600
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                enabled = answer.isNotBlank()
            ) {
                Text("保存", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
