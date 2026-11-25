package gal.horario.app

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Calendar

class MainActivity : Activity() {
    private val INK = Color.parseColor("#F4F0EA")
    private val MUT = Color.parseColor("#A4AAC8")
    private val CARD = Color.parseColor("#394068")
    private val DARK = Color.parseColor("#1D2137")
    private val REQ_IMPORT = 1
    private val REQ_EXPORT = 2
    private var items: MutableList<Item> = mutableListOf()
    private lateinit var tabs: LinearLayout
    private lateinit var list: LinearLayout
    private var day = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun round(c: Int, r: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }
    private fun lp(w: Int, h: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0, wt: Float = 0f) =
        LinearLayout.LayoutParams(w, h, wt).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.statusBarColor = Color.parseColor("#2B3050")
        window.navigationBarColor = DARK
        items = Data.items(this).toMutableList()
        val dow = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
        day = if (dow < 5) dow else 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.parseColor("#2B3050"), DARK))
            setPadding(dp(18), dp(14), dp(18), dp(10))
        }
        root.addView(TextView(this).apply {
            text = "O meu horario"; textSize = 30f; setTextColor(INK); typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "Elixe un día e toca unha clase para editala."; textSize = 14f; setTextColor(MUT)
        }, lp(-1, -2, b = 14))
        tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(tabs, lp(-1, -2, b = 12))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list); isVerticalScrollBarEnabled = false }, lp(-1, 0, wt = 1f))
        root.addView(TextView(this).apply {
            text = "+  Engadir clase"; textSize = 17f; setTextColor(DARK); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; background = round(Color.parseColor("#FFCF5C"), 18)
            setPadding(0, dp(15), 0, dp(15)); setOnClickListener { edit(null) }
        }, lp(-1, -2, t = 10))
        root.addView(TextView(this).apply {
            text = "Widgets: mantén premida a pantalla principal › Widgets › Horario"
            textSize = 12f; setTextColor(MUT); gravity = Gravity.CENTER
        }, lp(-1, -2, t = 8))
        setContentView(root)
        render()
    }

    private fun pill(t: String, fn: () -> Unit) = TextView(this).apply {
        text = t; textSize = 13f; setTextColor(INK); background = round(CARD, 14)
        setPadding(dp(14), dp(9), dp(14), dp(9)); setOnClickListener { fn() }
    }

    private fun render() {
        tabs.removeAllViews()
        Data.SHORT.forEachIndexed { i, n ->
            val on = i == day
            tabs.addView(TextView(this).apply {
                text = n; gravity = Gravity.CENTER; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (on) DARK else INK)
                background = round(if (on) INK else CARD, 14)
                setPadding(0, dp(11), 0, dp(11))
                setOnClickListener { day = i; render() }
            }, lp(0, -2, l = if (i == 0) 0 else 5, wt = 1f))
        }
        list.removeAllViews()
        val rows = items.withIndex().filter { it.value.day == day }.sortedBy { it.value.start }
        if (rows.isEmpty()) {
            list.addView(TextView(this).apply {
                text = if (items.isEmpty()) "O horario está baleiro.\nEngade clases ou importa un ficheiro .txt."
                       else "Sen clases este día."
                textSize = 15f; setTextColor(MUT); gravity = Gravity.CENTER
            }, lp(-1, -2, t = 50, b = 24))
        }
        rows.forEach { (idx, row) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = round(CARD, 18); setPadding(dp(12), dp(12), dp(16), dp(12))
                setOnClickListener { edit(idx) }
            }
            card.addView(View(this).apply { background = round(Data.color(row.name), 4) },
                LinearLayout.LayoutParams(dp(6), dp(46)).apply { rightMargin = dp(14) })
            val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            tx.addView(TextView(this).apply { text = row.name; textSize = 19f; setTextColor(INK); typeface = Typeface.DEFAULT_BOLD })
            if (row.detail.isNotEmpty()) tx.addView(TextView(this).apply { text = row.detail; textSize = 13f; setTextColor(MUT) })
            card.addView(tx, lp(0, -2, wt = 1f))
            card.addView(TextView(this).apply {
                text = "${Data.fmt(row.start)}\n${Data.fmt(row.end)}"; textSize = 13f; setTextColor(MUT); gravity = Gravity.END
            })
            list.addView(card, lp(-1, -2, b = 8))
        }
        val util = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        util.addView(pill("Importar txt") { startImport() }, lp(-2, -2, r = 6))
        util.addView(pill("Exportar txt") { startExport() }, lp(-2, -2, r = 6))
        util.addView(pill("Borrar todo") { confirmClear() })
        list.addView(util, lp(-1, -2, t = 10, b = 10))
    }

    private fun commit() { Data.saveItems(this, items); Refresh.all(this); render() }

    private fun startImport() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), REQ_IMPORT)
    }

    private fun startExport() {
        if (items.isEmpty()) { toast("Aínda non hai clases para exportar"); return }
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("text/plain").putExtra(Intent.EXTRA_TITLE, "horario.txt"), REQ_EXPORT)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (res != RESULT_OK) return
        try {
            if (req == REQ_EXPORT) {
                contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Data.exportText(items).toByteArray(Charsets.UTF_8)) }
                toast("Horario exportado")
            } else if (req == REQ_IMPORT) {
                val t = contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
                askImport(Data.parse(t.removePrefix("\uFEFF")))
            }
        } catch (e: Exception) { toast("Non se puido ler ou gardar o ficheiro") }
    }

    private fun askImport(n: List<Item>) {
        if (n.isEmpty()) { toast("Non atopei clases válidas no ficheiro"); return }
        if (items.isEmpty()) { items = n.toMutableList(); commit(); toast("Importadas ${n.size} clases"); return }
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle("Importar ${n.size} clases")
            .setMessage("Xa tes clases gardadas. Que queres facer?")
            .setPositiveButton("Substituír") { _, _ -> items = n.toMutableList(); commit() }
            .setNeutralButton("Engadir") { _, _ -> items = (items + n).distinct().toMutableList(); commit() }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun confirmClear() {
        if (items.isEmpty()) return
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setMessage("Queres borrar todas as clases? Podes exportalas antes.")
            .setPositiveButton("Borrar") { _, _ -> items.clear(); commit() }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun chip(t: String, c: Int, fn: () -> Unit) = TextView(this).apply {
        text = t; textSize = 13f; setTextColor(DARK); typeface = Typeface.DEFAULT_BOLD
        background = round(c, 12); setPadding(dp(12), dp(7), dp(12), dp(7)); setOnClickListener { fn() }
    }

    private fun edit(idx: Int?) {
        val old = idx?.let { items[it] }
        val slots = items.map { it.start to it.end }.distinct().sortedBy { it.first }
        val used = items.map { it.name to it.detail }.distinct().sortedBy { it.first.lowercase() }
        var st = old?.start ?: slots.firstOrNull()?.first ?: 480
        var en = old?.end ?: slots.firstOrNull()?.second ?: 530
        val sel = mutableSetOf(old?.day ?: day)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(6), dp(20), 0) }
        val name = EditText(this).apply { hint = "Materia"; setSingleLine(); setText(old?.name ?: "") }
        val det = EditText(this).apply { hint = "Grupos, aula... (opcional)"; setSingleLine(); setText(old?.detail ?: "") }
        val bs = Button(this); val be = Button(this)
        fun upd() { bs.text = "Inicio  ${Data.fmt(st)}"; be.text = "Fin  ${Data.fmt(en)}" }
        upd()
        fun pick(v: Boolean) {
            val cur = if (v) st else en
            TimePickerDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK, { _, h, m -> if (v) st = h * 60 + m else en = h * 60 + m; upd() }, cur / 60, cur % 60, true).show()
        }
        bs.setOnClickListener { pick(true) }; be.setOnClickListener { pick(false) }
        fun row(v: List<View>) = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(this@MainActivity).apply { v.forEach { addView(it, lp(-2, -2, r = 6)) } })
        }
        fun label(t: String, top: Int = 12) = TextView(this).apply { text = t; textSize = 12f; setTextColor(MUT) }.also { box.addView(it, lp(-1, -2, t = top, b = 4)) }

        label("Días (podes marcar varios)", 0)
        val dayRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val dayChips = Data.SHORT.mapIndexed { i, n -> TextView(this).apply {
            text = n; gravity = Gravity.CENTER; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(9), 0, dp(9))
        } }
        fun paintDays() = dayChips.forEachIndexed { i, t ->
            val on = i in sel
            t.setTextColor(if (on) DARK else INK); t.background = round(if (on) INK else CARD, 12)
        }
        dayChips.forEachIndexed { i, t ->
            t.setOnClickListener { if (i in sel) sel.remove(i) else sel.add(i); paintDays() }
            dayRow.addView(t, lp(0, -2, l = if (i == 0) 0 else 4, wt = 1f))
        }
        paintDays(); box.addView(dayRow)

        if (used.isNotEmpty()) {
            label("Materias que xa usaches (toca para copiar)")
            box.addView(row(used.map { (n, d) -> chip(if (d.isEmpty()) n else "$n · $d", Data.color(n)) {
                name.setText(n); det.setText(d); name.setSelection(n.length) } }))
        }
        label("Materia")
        box.addView(name); box.addView(det)
        if (slots.isNotEmpty()) {
            label("Franxas que xa usaches")
            box.addView(row(slots.map { (a, b) -> chip("${Data.fmt(a)}–${Data.fmt(b)}", Color.parseColor("#C9CEE8")) { st = a; en = b; upd() } }))
        }
        label("Hora")
        val tr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tr.addView(bs, lp(0, -2, wt = 1f)); tr.addView(be, lp(0, -2, wt = 1f))
        box.addView(tr)

        val b = AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(if (old == null) "Nova clase" else "Editar clase")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Gardar", null).setNegativeButton("Cancelar", null)
        if (idx != null) b.setNeutralButton("Eliminar") { _, _ -> items.removeAt(idx); commit() }
        val d = b.create(); d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nm = name.text.toString().trim().replace(";", ",")
            when {
                nm.isEmpty() -> toast("Pon un nome á materia")
                en <= st -> toast("A hora de fin debe ser despois da de inicio")
                sel.isEmpty() -> toast("Marca polo menos un día")
                else -> {
                    val dt = det.text.toString().trim().replace(";", ",")
                    if (idx != null) items.removeAt(idx)
                    sel.sorted().forEach { dd -> items.add(Item(dd, st, en, nm, dt)) }
                    items = items.distinct().toMutableList()
                    commit(); d.dismiss()
                }
            }
        }
    }
}
