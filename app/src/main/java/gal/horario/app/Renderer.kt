package gal.horario.app

import android.graphics.*

object Renderer {
    private const val S = Data.S
    private const val E = Data.E
    private val INK = Color.parseColor("#F3EFE6")
    private val MUT = Color.parseColor("#8B8FA6")
    private val LINE = Color.parseColor("#262A40")

    private fun paint(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c }
    private fun fit(p: Paint, t: String, maxW: Float, size: Float) {
        p.textSize = size
        while (p.measureText(t) > maxW && p.textSize > 7f) p.textSize -= 1f
    }
    private fun base(w: Int, h: Int): Pair<Bitmap, Canvas> {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = minOf(w, h) * 0.09f
        c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, paint(Color.parseColor(Data.BG)))
        return b to c
    }

    fun clock(w: Int, h: Int, items: List<Item>, day: Int, now: Int, live: Boolean): Bitmap {
        val (b, c) = base(w, h)
        val m = minOf(w, h).toFloat()
        val cx = w / 2f; val cy = h / 2f
        val rad = m * 0.36f; val sw = m * 0.085f
        val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        fun ang(x: Int) = -150f + (x - S) / (E - S).toFloat() * 300f
        val pa = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = sw; color = LINE }
        c.drawArc(oval, ang(S) - 90f, 300f, false, pa)
        val today = items.filter { it.day == day }
        today.forEach {
            pa.color = Data.color(it.name)
            val a0 = ang(it.start.coerceIn(S, E)); val a1 = ang(it.end.coerceIn(S, E))
            c.drawArc(oval, a0 - 89f, a1 - a0 - 2f, false, pa)
        }
        val cur = if (live) today.firstOrNull { now >= it.start && now < it.end } else null
        if (live) {
            val a = Math.toRadians(ang(now.coerceIn(S, E)).toDouble())
            val x = cx + (rad * Math.sin(a)).toFloat(); val y = cy - (rad * Math.cos(a)).toFloat()
            val lp = paint(INK).apply { style = Paint.Style.STROKE; strokeWidth = m * 0.012f; strokeCap = Paint.Cap.ROUND }
            c.drawLine(cx + (x - cx) * 0.62f, cy + (y - cy) * 0.62f, x, y, lp)
            c.drawCircle(x, y, sw * 0.42f, paint(INK))
        }
        val tp = paint(INK).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val sp = paint(MUT).apply { textAlign = Paint.Align.CENTER }
        val title = when {
            cur != null -> cur.name
            live && now < S -> "Antes de clase"
            live && now >= E -> "Rematou"
            live -> "Libre"
            else -> "${today.size} clases"
        }
        val small = if (live) "Agora" else Data.DAYS[day]
        val detail = if (cur != null) "quedan ${cur.end - now} min" else ""
        fit(tp, title, rad * 1.5f, rad * 0.3f)
        c.drawText(title, cx, cy + tp.textSize * 0.3f, tp)
        sp.textSize = rad * 0.17f
        c.drawText(small, cx, cy - rad * 0.42f, sp)
        c.drawText(detail, cx, cy + rad * 0.55f, sp)
        return b
    }

    fun schedule(w: Int, h: Int, items: List<Item>, today: Int, now: Int): Bitmap {
        val (b, c) = base(w, h)
        val pad = w * 0.025f; val hh = h * 0.12f; val lw = w * 0.07f
        val cw = (w - lw - pad) / 5f
        val top = pad + hh; val ht = h - top - pad
        fun y(x: Int) = top + (x.coerceIn(S, E) - S) / (E - S).toFloat() * ht
        val tp = paint(INK).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val mp = paint(MUT).apply { textAlign = Paint.Align.CENTER }
        for (d in 0..4) {
            val x0 = lw + d * cw
            if (d == today) c.drawRoundRect(x0 + 2, pad, x0 + cw - 2, pad + hh - 4, 10f, 10f, paint(INK))
            tp.color = if (d == today) Color.parseColor(Data.BG) else INK
            fit(tp, Data.SHORT[d], cw * 0.8f, hh * 0.45f)
            c.drawText(Data.SHORT[d], x0 + cw / 2, pad + hh * 0.62f, tp)
        }
        val lp = paint(LINE).apply { strokeWidth = 1f }
        mp.textSize = hh * 0.34f
        for (hr in 9..14) {
            c.drawLine(lw, y(hr * 60), w - pad, y(hr * 60), lp)
            c.drawText("$hr", lw / 2, y(hr * 60) + mp.textSize * 0.35f, mp)
        }
        val cp = Paint(Paint.ANTI_ALIAS_FLAG)
        val ip = paint(Color.parseColor(Data.BG)).apply { textAlign = Paint.Align.CENTER }
        items.forEach {
            val x0 = lw + it.day * cw + 2
            val r = RectF(x0, y(it.start) + 1, x0 + cw - 4, y(it.end) - 1)
            cp.color = Data.color(it.name)
            c.drawRoundRect(r, 9f, 9f, cp)
            if (r.height() > 18f) {
                ip.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                fit(ip, it.name, r.width() * 0.88f, minOf(r.height() * 0.4f, cw * 0.2f))
                val two = it.detail.isNotEmpty() && r.height() > ip.textSize * 2.6f
                val ty = if (two) r.centerY() - ip.textSize * 0.1f else r.centerY() + ip.textSize * 0.35f
                c.drawText(it.name, r.centerX(), ty, ip)
                if (two) {
                    val big = ip.textSize
                    ip.typeface = Typeface.DEFAULT
                    fit(ip, it.detail, r.width() * 0.88f, big * 0.62f)
                    c.drawText(it.detail, r.centerX(), ty + big * 0.9f, ip)
                }
            }
        }
        if (today in 0..4 && now in S..E) {
            val ly = y(now); val x0 = lw + today * cw
            val np = paint(Color.WHITE).apply { strokeWidth = 3f }
            c.drawLine(x0, ly, x0 + cw, ly, np)
            c.drawCircle(x0, ly, 6f, np)
        }
        return b
    }
}
