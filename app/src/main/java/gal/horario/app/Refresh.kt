package gal.horario.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.util.Calendar

object Refresh {
    fun all(ctx: Context) {
        val m = AppWidgetManager.getInstance(ctx)
        var n = 0
        listOf(ClockWidget::class.java to 0, ScheduleWidget::class.java to 1).forEach { (cls, kind) ->
            val ids = m.getAppWidgetIds(ComponentName(ctx, cls))
            n += ids.size
            ids.forEach { update(ctx, m, it, kind) }
        }
        if (n == 0) cancel(ctx)
    }

    fun update(ctx: Context, m: AppWidgetManager, id: Int, kind: Int) {
        val o = m.getAppWidgetOptions(id)
        val d = ctx.resources.displayMetrics.density
        var w = (o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200) * d).toInt().coerceAtLeast(100)
        var h = (o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200) * d).toInt().coerceAtLeast(100)
        val s = Math.sqrt(380_000.0 / (w.toDouble() * h))
        if (s < 1) { w = (w * s).toInt(); h = (h * s).toInt() }
        val cal = Calendar.getInstance()
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val items = Data.items(ctx)
        val bmp = if (kind == 0) Renderer.clock(w, h, items, if (dow < 5) dow else 0, now, dow < 5)
                  else Renderer.schedule(w, h, items, if (dow < 5) dow else -1, now)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_image)
        rv.setImageViewBitmap(R.id.img, bmp)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        rv.setOnClickPendingIntent(R.id.root, open)
        m.updateAppWidget(id, rv)
    }

    private fun pi(ctx: Context) = PendingIntent.getBroadcast(ctx, 1, Intent(ctx, TickReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + 60_000L, 5 * 60_000L, pi(ctx))
    }

    fun cancel(ctx: Context) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(ctx))
    }
}
