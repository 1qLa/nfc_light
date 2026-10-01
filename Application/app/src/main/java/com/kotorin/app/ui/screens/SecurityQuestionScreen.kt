package com.kotorin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotorin.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionScreen(
    onNavigateBack: () -> Unit,
    onRegisterComplete: () -> Unit
) {
    val questions = listOf(
        "初めて飼ったペットの名前は？",
        "母親の旧姓は？",
        "小学校の名前は？",
        "生まれた病院の名前は？",
        "子供の頃の一番好きな食べ物は？"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedQuestion by remember { mutableStateOf(questions[0]) }
    var answer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Gray600)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Emerald500, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Eco, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "秘密の質問", fontSize = 20.sp, color = Color.Black)

        Spacer(modifier = Modifier.height(8.dp))

        StepIndicator(currentStep = 3, totalSteps = 3)

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "パスワードリセット時に使用します", fontSize = 14.sp, color = Gray600)

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = "質問を選択", fontSize = 14.sp, color = Gray600)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Gray100,
                        unfocusedContainerColor = Gray100,
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "回答", fontSize = 14.sp, color = Gray600)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                placeholder = { Text("回答を入力してください") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Gray100,
                    unfocusedContainerColor = Gray100,
                    unfocusedBorderColor = Gray200,
                    focusedBorderColor = Emerald500
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRegisterComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                enabled = answer.isNotBlank()
            ) {
                Text(text = "登録完了", color = Color.White, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
