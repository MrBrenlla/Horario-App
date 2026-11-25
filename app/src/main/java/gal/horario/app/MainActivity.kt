package gal.horario.app

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(28))
            setBackgroundColor(Color.parseColor(Data.BG))
        }
        fun tv(t: String, size: Float, c: String, bold: Boolean = false) = TextView(this).apply {
            text = t; textSize = size; setTextColor(Color.parseColor(c))
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        }
        col.addView(tv("Horario", 36f, "#F3EFE6", true))
        col.addView(tv("Para engadir os widgets: mantén premida a pantalla principal, toca Widgets e busca Horario. " +
            "Hai dous: Reloxo do día e Horario semanal. Toca un widget para volver aquí.", 14f, "#8B8FA6"))
        col.addView(tv("Formato: día;inicio;fin;materia;detalle (unha clase por liña). Exemplo: Luns;9:15;10:05;tec1;1BACB", 13f, "#8B8FA6"))
        val et = EditText(this).apply {
            setText(Data.load(this@MainActivity))
            typeface = Typeface.MONOSPACE; textSize = 12f
            setTextColor(Color.parseColor("#F3EFE6")); setBackgroundColor(Color.parseColor("#151726"))
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 14
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        col.addView(et)
        val save = Button(this).apply {
            text = "Gardar e actualizar widgets"
            setOnClickListener {
                val n = Data.parse(et.text.toString()).size
                Data.save(this@MainActivity, et.text.toString())
                Refresh.all(this@MainActivity)
                Toast.makeText(this@MainActivity, "Gardado: $n clases", Toast.LENGTH_SHORT).show()
            }
        }
        val reset = Button(this).apply {
            text = "Restaurar horario orixinal"
            setOnClickListener {
                et.setText(Data.DEFAULT); Data.save(this@MainActivity, Data.DEFAULT); Refresh.all(this@MainActivity)
            }
        }
        col.addView(save); col.addView(reset)
        setContentView(ScrollView(this).apply { addView(col); setBackgroundColor(Color.parseColor(Data.BG)) })
    }
}
