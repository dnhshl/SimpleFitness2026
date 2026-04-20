package com.example.main.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.main.statemgmt.AppViewModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Speedometer Screen.
 * 
 * Instead of a library, we use a custom Canvas approach here. 
 * Why? Because it helps you understand how drawing works in Compose, 
 * which is a fundamental skill for custom UI components.
 */
@Composable
fun SpeedometerScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val puls = uiState.lastReceivedData?.puls ?: 0
    
    // LaunchedEffect starts polling when this screen is shown.
    // By using "Unit" as the key, it runs only once when entering the screen.
    LaunchedEffect(Unit) {
        viewModel.startPolling(intervalMs = 1000)
    }
    
    // DisposableEffect ensures we stop the background work when leaving.
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Current Heart Rate",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // The Speedometer Visual
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(300.dp)
        ) {
            SpeedometerView(value = puls.toFloat(), min = 40f, max = 180f)
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$puls",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 64.sp
                    )
                )
                Text(text = "BPM", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * A custom-drawn Speedometer using Compose Canvas.
 */
@Composable
fun SpeedometerView(
    value: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier
) {
    // Smoothly animate the needle movement
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(min, max),
        label = "NeedleAnimation"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerPoint = size.center
        val radius = size.minDimension / 2
        
        // 1. Draw the background arc (Gray path)
        drawArc(
            color = secondaryColor,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. Draw the colored "progress" arc
        val sweepAngle = ((animatedValue - min) / (max - min)) * 270f
        drawArc(
            color = primaryColor,
            startAngle = 135f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3. Draw the Needle (Red line)
        val angleInRadians = Math.toRadians((135f + sweepAngle).toDouble())
        val needleLength = radius * 0.9f
        val endX = centerPoint.x + needleLength * cos(angleInRadians).toFloat()
        val endY = centerPoint.y + needleLength * sin(angleInRadians).toFloat()

        drawLine(
            color = Color.Red,
            start = centerPoint,
            end = Offset(endX, endY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        drawCircle(color = Color.Red, radius = 8.dp.toPx(), center = centerPoint)
    }
}
