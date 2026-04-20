package com.example.main.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.main.statemgmt.AppViewModel
import com.example.main.statemgmt.TimeHelper
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent

@Composable
fun GraphScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }

    // Start polling when screen is entered
    LaunchedEffect(Unit) {
        viewModel.startPolling(intervalMs = 2000)
    }
    
    // Ensure polling stops when screen is left
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    // Update the chart whenever history changes
    LaunchedEffect(uiState.fitnessHistory) {
        if (uiState.fitnessHistory.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(uiState.fitnessHistory.map { it.puls })
                }
            }
        }
    }

    // d) Formatter for the X-Axis to show the time from the ISO timestamp
    val xAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
        uiState.fitnessHistory.getOrNull(value.toInt())?.let {
            TimeHelper.formatToTimeOnly(it.isotimestamp)
        } ?: ""
    }

    // Configure scroll state to automatically scroll to the end on new data
    val scrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End,
        autoScroll = Scroll.Absolute.End,
        autoScrollCondition = AutoScrollCondition.OnModelGrowth
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Heart Rate History", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.fitnessHistory.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Waiting for data...")
            }
        } else {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(Fill(MaterialTheme.colorScheme.primary)),
                                // a) Show a dot (point) for each measurement
                                pointProvider = LineCartesianLayer.PointProvider.single(
                                    LineCartesianLayer.Point(
                                        rememberShapeComponent(
                                            fill = Fill(MaterialTheme.colorScheme.primary),
                                            shape = CircleShape
                                        )
                                    )
                                )
                            )
                        ),
                        // b) Fixed Y-axis scale for pulse values (e.g., 40 to 160 BPM)
                        rangeProvider = CartesianLayerRangeProvider.fixed(minY = 40.0, maxY = 160.0)
                    ),
                    startAxis = VerticalAxis.rememberStart(),
                    // d) The tic marks on the x axis should reflect the time
                    bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xAxisValueFormatter),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().weight(1f),
                // c) Make the x axis scrollable and zoomable
                scrollState = scrollState,
                zoomState = rememberVicoZoomState(),
                // b) Fix: Drop the annoying "start from 0" animation by setting animationSpec to null
                animationSpec = null
            )
        }
    }
}
