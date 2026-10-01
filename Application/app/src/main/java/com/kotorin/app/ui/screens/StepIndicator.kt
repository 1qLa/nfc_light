package com.kotorin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kotorin.app.ui.theme.Emerald500
import com.kotorin.app.ui.theme.Gray200

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..totalSteps) {
            val active = i <= currentStep
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (active) Emerald500 else Gray200, CircleShape)
            )
            if (i < totalSteps) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(if (active && i < currentStep) Emerald500 else Gray200)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "STEP $currentStep / $totalSteps",
        fontSize = 11.sp,
        color = Emerald500
    )
}
