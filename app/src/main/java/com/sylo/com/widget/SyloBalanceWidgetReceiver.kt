package com.sylo.com.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** System entry point for the home-screen widget; delegates rendering to [SyloBalanceWidget]. */
class SyloBalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SyloBalanceWidget()
}
