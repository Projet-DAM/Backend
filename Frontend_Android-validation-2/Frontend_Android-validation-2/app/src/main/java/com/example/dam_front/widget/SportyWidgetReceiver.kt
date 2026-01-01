package com.example.dam_front.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SportyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SportyWidget()

    override fun onUpdate(
        context: android.content.Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d("SportyWidget", "📡 Receiver onUpdate for ${appWidgetIds.size} widgets")
        
        // On lance une coroutine pour forcer la regénération Glance
        kotlinx.coroutines.GlobalScope.launch {
            try {
                SportyWidget().updateAll(context)
                android.util.Log.d("SportyWidget", "✅ Receiver triggered updateAll successfully")
            } catch (e: Exception) {
                android.util.Log.e("SportyWidget", "❌ Receiver updateAll failed", e)
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        android.util.Log.d("SportyWidget", "📡 Receiver onReceive: ${intent.action}")
        super.onReceive(context, intent)
    }
}
