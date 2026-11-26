package gal.horario.app

import android.graphics.*
import android.graphics.drawable.Drawable

/** Debuxa raias diagonais dentro dun rectángulo con cantos redondeados. */
fun drawStripes(c: Canvas, r: RectF, rad: Float, line: Int, step: Float, w: Float) {
    c.save()
    val path = Path(); path.addRoundRect(r, rad, rad, Path.Direction.CW); c.clipPath(path)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = w; color = line }
    var x = r.left - r.height()
    while (x < r.right) { c.drawLine(x, r.bottom, x + r.height(), r.top, p); x += step }
    c.restore()
}

class StripeDrawable(private val base: Int, private val line: Int, private val rad: Float,
                     private val step: Float, private val w: Float) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(c: Canvas) {
        val r = RectF(bounds)
        p.style = Paint.Style.FILL; p.color = base
        c.drawRoundRect(r, rad, rad, p)
        drawStripes(c, r, rad, line, step, w)
    }
    override fun setAlpha(a: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
