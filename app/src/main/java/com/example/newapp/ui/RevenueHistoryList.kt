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
    history: List<RevenueSnapshot>,
    currency: String,
    modifier: Modifier = Modifier
) {
    // Show only last 7 entries for brevity, or all if preferred. 
    // User asked for "On this day...", implying a daily breakdown.
    // Let's reverse to show newest first.
    val sortedHistory = remember(history) { history.sortedByDescending { it.timestamp } }

    Column(modifier = modifier) {
        if (sortedHistory.isEmpty()) {
            Text("No history available yet.", style = MaterialTheme.typography.bodyMedium)
        } else {
            sortedHistory.forEach { snapshot ->
                HistoryItem(snapshot, currency)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun HistoryItem(snapshot: RevenueSnapshot, currency: String) {
    val total = snapshot.modrinthRevenue + snapshot.curseForgeRevenue
    val dateString = remember(snapshot.timestamp) {
        val date = Date(snapshot.timestamp)
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
                text = "Daily Total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "+${formatCurrency(total, currency)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
