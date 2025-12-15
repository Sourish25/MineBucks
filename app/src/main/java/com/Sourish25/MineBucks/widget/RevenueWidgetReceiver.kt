package com.Sourish25.MineBucks.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

class RevenueWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RevenueWidget()
    
    companion object {
        val TOTAL_REVENUE_KEY = doublePreferencesKey("total_revenue")
        val CURRENCY_KEY = stringPreferencesKey("currency")
    }
}
