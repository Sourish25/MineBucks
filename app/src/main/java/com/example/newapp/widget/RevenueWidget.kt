package com.example.newapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.FontWeight
import androidx.glance.appwidget.cornerRadius
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity

class RevenueWidget : GlanceAppWidget() {

    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
             GlanceTheme {
                RevenueWidgetContent()
            }
        }
    }
    
    @Composable
    private fun OutlinedText(
        text: String,
        style: TextStyle,
        outlineColor: androidx.glance.unit.ColorProvider,
        modifier: GlanceModifier = GlanceModifier
    ) {
        androidx.glance.layout.Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            // Outlines (Shifted via Padding)
            // Note: Padding pushes content inward, so to shift "Left" we pad "Right"
            Text(text = text, style = style.copy(color = outlineColor), modifier = GlanceModifier.padding(bottom = 2.dp, end = 2.dp)) // Top-Left
            Text(text = text, style = style.copy(color = outlineColor), modifier = GlanceModifier.padding(top = 2.dp, end = 2.dp))    // Bottom-Left
            Text(text = text, style = style.copy(color = outlineColor), modifier = GlanceModifier.padding(bottom = 2.dp, start = 2.dp)) // Top-Right
            Text(text = text, style = style.copy(color = outlineColor), modifier = GlanceModifier.padding(top = 2.dp, start = 2.dp))    // Bottom-Right
            
            // Main Text (Centered)
            Text(text = text, style = style, modifier = GlanceModifier.padding(1.dp)) 
        }
    }

    @Composable
    private fun RevenueWidgetContent() {
        val totalRevenue = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()[RevenueWidgetReceiver.TOTAL_REVENUE_KEY] ?: 0.0
        val currency = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()[RevenueWidgetReceiver.CURRENCY_KEY] ?: "USD"
        
        // Responsive Layout Logic
        val size = androidx.glance.LocalSize.current
        val isSmall = size.width < 140.dp || size.height < 100.dp
        
        val formatted = if (currency == "USD") "$%.2f".format(totalRevenue) else "$totalRevenue"

        // Dynamic Text Sizes
        val moneySize = if (isSmall) 20.sp else 32.sp
        val labelSize = if (isSmall) 10.sp else 12.sp

        androidx.glance.layout.Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface) // Dynamic Dark/Light Theme Background
                .cornerRadius(24.dp)
                .clickable(androidx.glance.action.actionStartActivity<com.example.newapp.MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            // Background Faded Block
            androidx.glance.Image(
                provider = androidx.glance.ImageProvider(com.example.newapp.R.drawable.ic_faded_block),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize().padding(16.dp),
                contentScale = androidx.glance.layout.ContentScale.Fit,
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant) // Tint layout to theme
            )

            // Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedText(
                    text = "REVENUE",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant, // Theme-aware subtle label
                        fontSize = labelSize,
                        fontWeight = FontWeight.Bold
                    ),
                    outlineColor = GlanceTheme.colors.surface
                )
                OutlinedText(
                    text = formatted,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary, // Theme-aware Bold Accent
                        fontSize = moneySize, 
                        fontWeight = FontWeight.Bold
                    ),
                    outlineColor = GlanceTheme.colors.surface
                )
                if (currency != "USD") {
                     Text(
                        text = currency,
                        style = TextStyle(
                            color = GlanceTheme.colors.outline,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
    companion object {
        suspend fun updateRevenueWidget(context: Context, total: Double, currency: String) {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            val widget = RevenueWidget()
            val glanceIds = manager.getGlanceIds(widget.javaClass)
            
            glanceIds.forEach { glanceId ->
                androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[com.example.newapp.widget.RevenueWidgetReceiver.TOTAL_REVENUE_KEY] = total
                    prefs[com.example.newapp.widget.RevenueWidgetReceiver.CURRENCY_KEY] = currency
                }
                widget.update(context, glanceId)
            }
        }
    }
}
