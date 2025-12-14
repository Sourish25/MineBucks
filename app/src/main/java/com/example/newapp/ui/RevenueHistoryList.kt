package com.example.newapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newapp.data.database.RevenueSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RevenueHistoryList(
    history: List<DailyRevenue>, // Updated type
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (history.isEmpty()) {
            Text(androidx.compose.ui.res.stringResource(com.example.newapp.R.string.no_recent_activity), style = MaterialTheme.typography.bodyMedium)
        } else {
            history.forEach { daily ->
                HistoryItem(daily, currency)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun HistoryItem(daily: DailyRevenue, currency: String) {
    val dateString = remember(daily.dateMillis) {
        val date = Date(daily.dateMillis)
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.newapp.R.string.daily_total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = (if (daily.amount >= 0) "+" else "") + formatCurrency(daily.amount, currency),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
