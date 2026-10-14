package com.example.main.ui.screens

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.main.R
import com.example.main.statemgmt.AppViewModel
import com.example.main.statemgmt.TimeHelper
import com.patrykandpatrick.vico.compose.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.BaseAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
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
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent

/**
 * LEARNING POINT: The Graph Screen with Session & Live Filtering
 * Displays real-time or historic Vico charts filtered by session.
 */
@Composable
fun GraphScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val graphData by viewModel.activeGraphData.collectAsState()
    val sessions by viewModel.availableSessions.collectAsState()
    val currentSelectedSession = uiState.selectedSessionId ?: viewModel.currentSessionId
    val modelProducer = remember { CartesianChartModelProducer() }
    
    val labelTime = stringResource(R.string.label_axis_time)
    val labelBpm = stringResource(R.string.label_axis_heart_rate)
    
    val axisTitleComponent = rememberTextComponent(
        style = TextStyle(color = MaterialTheme.colorScheme.onSurface),
        margins = Insets(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    )
    
    val axisLabelComponent = rememberAxisLabelComponent(
        style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
        minWidth = TextComponent.MinWidth.fixed(32.dp)
    )

    LaunchedEffect(graphData) {
        if (graphData.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = graphData.map { TimeHelper.isoToEpochSeconds(it.isotimestamp) },
                        y = graphData.map { it.puls }
                    )
                }
            }
        }
    }

    val xAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
        TimeHelper.formatEpochToTime(value)
    }

    val scrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End,
        autoScroll = if (uiState.showLiveGraph) Scroll.Absolute.End else Scroll.Absolute.Start,
        autoScrollCondition = if (uiState.showLiveGraph) AutoScrollCondition.OnModelGrowth else AutoScrollCondition.Never
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- DATA SOURCE SELECTOR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (uiState.showLiveGraph) "LIVE DATA (Memory)" else "HISTORIC DATA (Database)",
                style = MaterialTheme.typography.titleMedium,
                color = if (uiState.showLiveGraph) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Switch(
                checked = uiState.showLiveGraph,
                onCheckedChange = { viewModel.setGraphSource(it) }
            )
        }

        // --- SESSION SELECTOR (Historic Data mode only) ---
        if (!uiState.showLiveGraph) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Training Session", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text(text = currentSelectedSession)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            val allSessions = if (sessions.isEmpty()) listOf(viewModel.currentSessionId) else sessions
                            allSessions.forEach { session ->
                                DropdownMenuItem(
                                    text = { Text(session) },
                                    onClick = {
                                        viewModel.setSelectedSessionId(session)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (graphData.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(if (uiState.showLiveGraph) "Waiting for live data..." else "No historic data found for this session.")
            }
        } else {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(Fill(MaterialTheme.colorScheme.primary)),
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
                        rangeProvider = CartesianLayerRangeProvider.fixed(minY = 40.0, maxY = 160.0)
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        label = axisLabelComponent,
                        itemPlacer = VerticalAxis.ItemPlacer.step(step = { 20.0 }),
                        title = { labelBpm },
                        titleComponent = axisTitleComponent,
                        size = BaseAxis.Size.Fixed(72.dp)
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        label = axisLabelComponent,
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                            spacing = { 5 },
                            addExtremeLabelPadding = true
                        ),
                        title = { labelTime },
                        titleComponent = axisTitleComponent,
                        valueFormatter = xAxisValueFormatter
                    ),
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().weight(1f),
                scrollState = scrollState,
                zoomState = rememberVicoZoomState(),
                animationSpec = spring(),
                animateIn = false
            )
        }
    }
}
