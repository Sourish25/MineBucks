package com.example.newapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newapp.data.database.RevenueSnapshot
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RevenueHistoryGraph(
    history: List<RevenueSnapshot>,
    modifier: Modifier = Modifier,
    lineColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    gradientColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
) {
    if (history.isEmpty()) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier,
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "No graph data available",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Transform Data to Vico Entries
    // Sort by timestamp just in case
    val sortedHistory = remember(history) { history.sortedBy { it.timestamp } }
    
    // Map snapshots to Chart Entries (x = index, y = total revenue)
    val entries = remember(sortedHistory) {
        sortedHistory.mapIndexed { index, snapshot ->
            FloatEntry(
                x = index.toFloat(),
                y = (snapshot.modrinthRevenue + snapshot.curseForgeRevenue).toFloat()
            )
        }
    }

    val chartEntryModel = remember(entries) { entryModelOf(entries) }

    Chart(
        chart = lineChart(
            lines = listOf(
                com.patrykandpatrick.vico.core.chart.line.LineChart.LineSpec(
                    lineColor = lineColor.toArgb(),
                    lineBackgroundShader = com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient(
                        arrayOf(gradientColor.copy(alpha = 0.5f), gradientColor.copy(alpha = 0.1f))
                    )
                )
            )
        ),
        model = chartEntryModel,
        startAxis = com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis(
            axis = com.patrykandpatrick.vico.compose.component.lineComponent(
                color = lineColor,
                thickness = 2.dp
            ),
            label = com.patrykandpatrick.vico.compose.component.textComponent(
                color = lineColor,
                textSize = 12.sp,
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            )
        ),
        bottomAxis = com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis(
            valueFormatter = { value, _ ->
                val index = value.toInt()
                if (index in sortedHistory.indices) {
                    val date = Date(sortedHistory[index].timestamp)
                    SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
                } else {
                    ""
                }
            },
            axis = com.patrykandpatrick.vico.compose.component.lineComponent(
                color = lineColor,
                thickness = 2.dp
            ),
            label = com.patrykandpatrick.vico.compose.component.textComponent(
                color = lineColor,
                textSize = 12.sp,
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            )
        ),
        modifier = modifier
    )
}
