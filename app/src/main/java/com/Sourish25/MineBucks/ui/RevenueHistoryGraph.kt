package com.Sourish25.MineBucks.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Sourish25.MineBucks.data.database.RevenueSnapshot
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
    dailyHistory: List<DailyRevenue>,
    modifier: Modifier = Modifier,
    lineColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    gradientColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
) {
    if (dailyHistory.isEmpty()) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier,
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(com.Sourish25.MineBucks.R.string.no_graph_data),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Transform Data to Vico Entries
    val entries = remember(dailyHistory) {
        dailyHistory.mapIndexed { index, daily ->
            FloatEntry(
                x = index.toFloat(),
                y = daily.amount.toFloat()
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
                if (index in dailyHistory.indices) {
                    val date = Date(dailyHistory[index].dateMillis)
                    SimpleDateFormat("EEE", Locale.getDefault()).format(date) // Day Name (Mon, Tue)
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
