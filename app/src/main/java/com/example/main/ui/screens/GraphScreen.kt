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

@Composable
fun GraphScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val graphData by viewModel.activeGraphData.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }
    
    // LEARNING POINT: Vico Axis Customization
    // We define titles and labels for the axes using string resources.
    val labelTime = stringResource(R.string.label_axis_time)
    val labelBpm = stringResource(R.string.label_axis_heart_rate)
    
    // We create a reusable 'TextComponent' for the titles.
    // Notice the 'margins' - this prevents the title from overlapping the tick labels.
    val axisTitleComponent = rememberTextComponent(
        style = TextStyle(color = MaterialTheme.colorScheme.onSurface),
        margins = Insets(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    )
    
    // We also style the tick labels (the numbers).
    // 'minWidth' is used here to prevent the axis from "jittering" (flicker)
    // when values change between 2 and 3 digits.
    val axisLabelComponent = rememberAxisLabelComponent(
        style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
        minWidth = TextComponent.MinWidth.fixed(32.dp)
    )

    // Polling is now managed globally by the Master Switch on the Home Screen.
    // No more screen-specific start/stop logic here.

    // Update the chart whenever active data changes
    LaunchedEffect(graphData) {
        if (graphData.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    // LEARNING POINT: Real-Time X-Axis
                    // Instead of using indices (0, 1, 2...), we use epoch seconds as X values.
                    // This means a 10-second gap looks twice as wide as a 5-second gap.
                    series(
                        x = graphData.map { TimeHelper.isoToEpochSeconds(it.isotimestamp) },
                        y = graphData.map { it.puls }
                    )
                }
            }
        }
    }

    // d) Formatter for the X-Axis to show the time from the epoch seconds
    val xAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
        TimeHelper.formatEpochToTime(value)
    }

    // LEARNING POINT: Dynamic Scrolling
    // When in "Live" mode, we want the graph to follow the latest data (AutoScroll).
    // When in "Database" mode, we disable auto-scroll so the user can inspect history.
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

        if (graphData.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(if (uiState.showLiveGraph) "Waiting for live data..." else "No historic data found.")
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
                    // LEARNING POINT: Vertical Axis (Y-Axis)
                    startAxis = VerticalAxis.rememberStart(
                        label = axisLabelComponent,
                        // 'step' allows us to control the interval of the labels.
                        // Here, we show a number every 20 BPM.
                        itemPlacer = VerticalAxis.ItemPlacer.step(step = { 20.0 }),
                        title = { labelBpm },
                        titleComponent = axisTitleComponent,
                        // STABILITY FIX: By giving the axis a fixed width, the graph doesn't 
                        // jump left and right when heart rate changes (the "flicker" effect).
                        size = BaseAxis.Size.Fixed(72.dp)
                    ),
                    // LEARNING POINT: Horizontal Axis (X-Axis)
                    bottomAxis = HorizontalAxis.rememberBottom(
                        label = axisLabelComponent,
                        // 'aligned' allows us to show a label every N data points.
                        // Here, we show a timestamp every 5 measurements.
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
                // c) Make the x axis scrollable and zoomable
                scrollState = scrollState,
                zoomState = rememberVicoZoomState(),
                // b) Use spring for smooth updates (fixes flicker) and disable initial "reveal" animation
                animationSpec = spring(),
                animateIn = false
            )
        }
    }
}
