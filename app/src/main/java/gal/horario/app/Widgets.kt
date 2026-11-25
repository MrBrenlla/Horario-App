package gal.horario.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

abstract class BaseWidget(private val kind: Int) : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, m: AppWidgetManager, ids: IntArray) {
        ids.forEach { Refresh.update(ctx, m, it, kind) }
        Refresh.schedule(ctx)
    }
    override fun onAppWidgetOptionsChanged(ctx: Context, m: AppWidgetManager, id: Int, o: Bundle) {
        Refresh.update(ctx, m, id, kind)
    }
}

class ClockWidget : BaseWidget(0)
class ScheduleWidget : BaseWidget(1)

class TickReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) { Refresh.all(ctx) }
}
