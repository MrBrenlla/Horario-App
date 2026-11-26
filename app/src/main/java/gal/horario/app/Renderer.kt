package gal.horario.app

import android.graphics.*

object Renderer {
    private val INK = Color.parseColor("#F4F0EA")
    private val MUT = Color.parseColor("#A4AAC8")
    private val LINE = Color.parseColor("#3B4268")
    private val DARK = Color.parseColor("#1D2137")

    private fun paint(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c }
    private fun glyph(c: Canvas, k: Int, x: Float, y: Float, r: Float, p: Paint) {
        val q = Paint(p); q.style = Paint.Style.FILL
        val pa = Path()
        when (k) {
            0 -> c.drawCircle(x, y, r, q)
            1 -> c.drawRect(x - r * .85f, y - r * .85f, x + r * .85f, y + r * .85f, q)
            2 -> { pa.moveTo(x, y - r); pa.lineTo(x + r, y + r * .8f); pa.lineTo(x - r, y + r * .8f); pa.close(); c.drawPath(pa, q) }
            3 -> { pa.moveTo(x, y - r); pa.lineTo(x + r, y); pa.lineTo(x, y + r); pa.lineTo(x - r, y); pa.close(); c.drawPath(pa, q) }
            4 -> { val t = r * .35f; c.drawRect(x - r, y - t, x + r, y + t, q); c.drawRect(x - t, y - r, x + t, y + r, q) }
            5 -> {
                for (i in 0 until 10) {
                    val a = Math.PI * i / 5 - Math.PI / 2; val rr = if (i % 2 == 0) r else r * .45f
                    val px = (x + rr * Math.cos(a)).toFloat(); val py = (y + rr * Math.sin(a)).toFloat()
                    if (i == 0) pa.moveTo(px, py) else pa.lineTo(px, py)
                }
                pa.close(); c.drawPath(pa, q)
            }
            6 -> { pa.moveTo(x, y + r); pa.lineTo(x + r, y - r * .8f); pa.lineTo(x - r, y - r * .8f); pa.close(); c.drawPath(pa, q) }
            7 -> { q.style = Paint.Style.STROKE; q.strokeWidth = r * .4f; c.drawCircle(x, y, r * .8f, q) }
            8 -> c.drawArc(RectF(x - r, y - r, x + r, y + r), 90f, 180f, true, q)
            9 -> { q.style = Paint.Style.STROKE; q.strokeWidth = r * .4f; c.drawRect(x - r * .75f, y - r * .75f, x + r * .75f, y + r * .75f, q) }
            10 -> { val t = r * .3f; c.save(); c.rotate(45f, x, y); c.drawRect(x - r * 1.1f, y - t, x + r * 1.1f, y + t, q); c.drawRect(x - t, y - r * 1.1f, x + t, y + r * 1.1f, q); c.restore() }
            11 -> { pa.moveTo(x - r * .8f, y - r); pa.lineTo(x + r, y); pa.lineTo(x - r * .8f, y + r); pa.close(); c.drawPath(pa, q) }
            else -> c.drawCircle(x, y, r, q)
        }
    }

    private fun nextItem(items: List<Item>, dow: Int, now: Int): Pair<Item, Int>? {
        for (k in 0..7) {
            val d = (dow + k) % 7
            if (d > 4) continue
            val c = items.filter { it.day == d && (k > 0 || it.start > now) }.minByOrNull { it.start }
            if (c != null) return c to k
        }
        return null
    }

    private fun fit(p: Paint, t: String, maxW: Float, size: Float) {
        p.textSize = size
        while (p.measureText(t) > maxW && p.textSize > 7f) p.textSize -= 1f
    }
    private fun base(w: Int, h: Int): Pair<Bitmap, Canvas> {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = minOf(w, h) * 0.09f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), Color.parseColor("#363C63"), Color.parseColor("#1F2339"), Shader.TileMode.CLAMP)
        c.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), r, r, p)
        return b to c
    }

    fun clock(w: Int, h: Int, items: List<Item>, day: Int, now: Int, live: Boolean, dow: Int): Bitmap {
        val (S, E) = Data.range(items)
        val (b, c) = base(w, h)
        val tall = h > w * 1.3f
        val wide = w > h * 1.45f
        val m = when { tall -> w.toFloat(); wide -> h.toFloat(); else -> minOf(w, h).toFloat() }
        val cx = if (wide) m / 2f else w / 2f
        val cy = if (tall) m / 2f else h / 2f
        val rad = m * 0.35f; val sw = m * 0.085f
        val oval = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        fun ang(x: Int) = -150f + (x - S) / (E - S).toFloat() * 300f
        val pa = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = sw; color = LINE }
        c.drawArc(oval, ang(S) - 90f, 300f, false, pa)
        val today = items.filter { it.day == day }.sortedBy { it.start }
        today.forEach {
            pa.color = Data.color(it.name)
            val a0 = ang(it.start.coerceIn(S, E)); val a1 = ang(it.end.coerceIn(S, E))
            c.drawArc(oval, a0 - 89f, a1 - a0 - 2f, false, pa)
            if (it.other) {
                val dp2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = sw; color = Color.argb(130, 29, 33, 55)
                    pathEffect = DashPathEffect(floatArrayOf(sw * 0.25f, sw * 0.3f), 0f)
                }
                c.drawArc(oval, a0 - 89f, a1 - a0 - 2f, false, dp2)
            }
            if (a1 - a0 > 9f) {
                val q = Math.toRadians(((a0 + a1) / 2f).toDouble())
                glyph(c, Data.shape(it.name), cx + (rad * Math.sin(q)).toFloat(), cy - (rad * Math.cos(q)).toFloat(), sw * 0.26f, paint(Data.textOn(Data.color(it.name))))
            }
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
        fit(tp, title, rad * 1.5f, rad * 0.3f)
        sp.textSize = rad * 0.16f
        c.drawText(if (live) "Agora" else Data.DAYS[day], cx, cy - rad * 0.5f, sp)
        c.drawText(title, cx, cy - rad * 0.1f + tp.textSize * 0.3f, tp)
        if (cur != null) {
            val info = listOf(Data.roomLabel(cur.room), "quedan ${cur.end - now} min").filter { s -> s.isNotEmpty() }.joinToString(" · ")
            fit(sp, info, rad * 1.4f, rad * 0.16f)
            c.drawText(info, cx, cy + rad * 0.27f, sp)
        }
        val nextS = nextItem(items, dow, now)
        if (nextS != null) {
            val (ni, k) = nextS
            val pre = when (k) { 0 -> ""; 1 -> "Mañá "; else -> Data.SHORT[ni.day] + " " }
            val rl = Data.roomLabel(ni.room)
            val txt = "Seguinte · $pre${Data.fmt(ni.start)} ${ni.name}" + (if (rl.isEmpty()) "" else " · $rl")
            val np2 = paint(INK).apply { textAlign = Paint.Align.CENTER }
            fit(np2, txt, rad * 1.25f, rad * 0.15f)
            c.drawText(txt, cx, cy + rad * 0.62f, np2)
        }

        if (tall || wide) {
            val pad = m * 0.06f
            val rg = if (tall) RectF(pad, m * 0.98f, w - pad, h - pad) else RectF(m * 0.98f, pad, w - pad, h - pad)
            val shown = if (live) today.filter { it.end > now } else today
            val ts = m * 0.07f; val rowH = ts * 2.3f
            val max = (rg.height() / rowH).toInt()
            val np = paint(MUT).apply { textSize = ts }
            if (shown.isEmpty()) c.drawText("Sen máis clases hoxe", rg.left + 6f, rg.top + ts * 1.4f, np)
            var y = rg.top
            shown.take(max).forEach {
                val isCur = live && now >= it.start && now < it.end
                if (isCur) c.drawRoundRect(rg.left, y, rg.right, y + rowH - 4f, 14f, 14f, paint(Color.argb(38, 255, 255, 255)))
                glyph(c, Data.shape(it.name), rg.left + 16f, y + rowH / 2f - 2f, ts * 0.38f, paint(Data.color(it.name)))
                val base = y + rowH / 2f + ts * 0.3f
                np.textSize = ts * 0.78f
                c.drawText(Data.fmt(it.start), rg.left + 32f, base, np)
                val nx = rg.left + 32f + ts * 3.1f
                val nm = paint(INK).apply { typeface = Typeface.create(Typeface.DEFAULT, if (it.other) Typeface.BOLD_ITALIC else Typeface.BOLD) }
                fit(nm, it.name, rg.right - nx - 6f, ts)
                c.drawText(it.name, nx, base, nm)
                val sub = listOf(it.detail, Data.roomLabel(it.room)).filter { s -> s.isNotEmpty() }.joinToString(" · ")
                if (sub.isNotEmpty()) {
                    val dx = nx + nm.measureText(it.name) + ts * 0.5f
                    val room = rg.right - dx - 6f
                    if (room > ts * 2) {
                        val dp = paint(MUT)
                        fit(dp, sub, room, ts * 0.72f)
                        c.drawText(sub, dx, base, dp)
                    }
                }
                y += rowH
            }
        }
        return b
    }

    fun schedule(w: Int, h: Int, items: List<Item>, today: Int, now: Int): Bitmap {
        val (S, E) = Data.range(items)
        val (b, c) = base(w, h)
        val pad = w * 0.025f; val hh = h * 0.11f; val lw = w * 0.07f
        val cw = (w - lw - pad) / 5f
        val top = pad + hh; val ht = h - top - pad
        fun y(x: Int) = top + (x.coerceIn(S, E) - S) / (E - S).toFloat() * ht
        val tp = paint(INK).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val mp = paint(MUT).apply { textAlign = Paint.Align.CENTER }
        for (d in 0..4) {
            val x0 = lw + d * cw
            if (d == today) c.drawRoundRect(x0 + 2, pad, x0 + cw - 2, pad + hh - 4, 12f, 12f, paint(INK))
            tp.color = if (d == today) DARK else INK
            fit(tp, Data.SHORT[d], cw * 0.8f, hh * 0.45f)
            c.drawText(Data.SHORT[d], x0 + cw / 2, pad + hh * 0.62f, tp)
        }
        val lp = paint(LINE).apply { strokeWidth = 1f }
        mp.textSize = hh * 0.34f
        for (hr in ((S + 59) / 60)..(E / 60)) {
            c.drawLine(lw, y(hr * 60), w - pad, y(hr * 60), lp)
            c.drawText("$hr", lw / 2, y(hr * 60) + mp.textSize * 0.35f, mp)
        }
        val cp = Paint(Paint.ANTI_ALIAS_FLAG)
        val ip = paint(DARK).apply { textAlign = Paint.Align.CENTER }
        items.forEach {
            val x0 = lw + it.day * cw + 2
            val r = RectF(x0, y(it.start) + 1, x0 + cw - 4, y(it.end) - 1)
            cp.color = Data.color(it.name)
            c.drawRoundRect(r, 10f, 10f, cp)
            if (it.other) drawStripes(c, r, 10f, Color.argb(100, 29, 33, 55), 12f, 4f)
            if (r.height() > 22f) glyph(c, Data.shape(it.name), r.left + minOf(cw * 0.1f, 14f), r.top + minOf(cw * 0.1f, 14f), minOf(cw * 0.05f, 8f), paint(Data.textOn(cp.color)))
            if (r.height() > 18f) {
                ip.color = Data.textOn(cp.color)
                ip.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                fit(ip, it.name, r.width() * 0.88f, minOf(r.height() * 0.4f, cw * 0.2f))
                val big = ip.textSize
                val extra = listOf(it.detail, Data.roomLabel(it.room)).filter { s -> s.isNotEmpty() }
                var n = extra.size
                while (n > 0 && big * 1.1f + n * big * 0.78f > r.height() - 6f) n--
                val totalH = big * 1.1f + n * big * 0.78f
                var by = r.centerY() - totalH / 2f + big * 0.9f
                c.drawText(it.name, r.centerX(), by, ip)
                ip.typeface = Typeface.DEFAULT
                for (k in 0 until n) {
                    by += big * 0.78f
                    fit(ip, extra[k], r.width() * 0.88f, big * 0.62f)
                    c.drawText(extra[k], r.centerX(), by, ip)
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
