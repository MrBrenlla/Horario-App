package gal.horario.app

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
                background = if (row.other) StripeDrawable(CARD, Color.argb(40, 255, 255, 255), dp(18).toFloat(), dp(10).toFloat(), dp(3).toFloat()) else round(CARD, 18)
                setPadding(dp(12), dp(12), dp(16), dp(12))
                setOnClickListener { edit(idx) }
            }
            card.addView(View(this).apply { background = round(Data.color(row.name), 4) },
                LinearLayout.LayoutParams(dp(6), dp(46)).apply { rightMargin = dp(14) })
            val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            tx.addView(TextView(this).apply { text = "${Data.GLYPHS[Data.shape(row.name)]}  ${row.name}"; textSize = 19f; setTextColor(INK); typeface = Typeface.DEFAULT_BOLD })
            val sub = listOf(row.detail, Data.roomLabel(row.room)).filter { s -> s.isNotEmpty() }.joinToString(" · ")
            if (sub.isNotEmpty()) tx.addView(TextView(this).apply { text = sub; textSize = 13f; setTextColor(MUT) })
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
        text = t; textSize = 13f; setTextColor(Data.textOn(c)); typeface = Typeface.DEFAULT_BOLD
        background = round(c, 12); setPadding(dp(12), dp(7), dp(12), dp(7)); setOnClickListener { fn() }
    }

    private class Sess(var day: Int, var st: Int, var en: Int, var room: String = "")

    private fun pickTime(cur: Int, fn: (Int) -> Unit) =
        TimePickerDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK, { _, h, m -> fn(h * 60 + m) }, cur / 60, cur % 60, true).show()

    private fun edit(idx: Int?) {
        val old = idx?.let { items[it] }
        val group = if (old == null) emptyList() else items.filter { it.name == old.name && it.detail == old.detail }
        val slots = items.map { it.start to it.end }.distinct().sortedBy { it.first }
        val used = items.map { it.name to it.detail }.distinct().sortedBy { it.first.lowercase() }
        val sess = mutableListOf<Sess>()
        var picked: Int? = old?.let { Data.shape(it.name) }
        var other = old?.other ?: false
        var refresh: () -> Unit = {}
        if (old == null) {
            val s0 = slots.firstOrNull()
            sess.add(Sess(day, s0?.first ?: 480, s0?.second ?: 530))
        } else group.sortedWith(compareBy({ it.day }, { it.start })).forEach { sess.add(Sess(it.day, it.start, it.end, it.room)) }
        var active = if (old == null) 0 else sess.indexOfFirst { it.day == old.day && it.st == old.start }.coerceAtLeast(0)

        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(6), dp(20), 0) }
        val name = EditText(this).apply { hint = "Materia"; setSingleLine(); setText(old?.name ?: "") }
        val det = EditText(this).apply { hint = "Grupos, aula... (opcional)"; setSingleLine(); setText(old?.detail ?: "") }
        fun label(t: String, top: Int = 12) { box.addView(TextView(this).apply { text = t; textSize = 12f; setTextColor(MUT) }, lp(-1, -2, t = top, b = 4)) }
        fun hrow(v: List<View>) = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(LinearLayout(this@MainActivity).apply { v.forEach { addView(it, lp(-2, -2, r = 6)) } })
        }
        fun sp(t: String, fn: () -> Unit) = TextView(this).apply {
            text = t; textSize = 15f; setTextColor(INK); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            background = round(Color.parseColor("#2B3050"), 10); setPadding(0, dp(10), 0, dp(10)); setOnClickListener { fn() }
        }

        val sessBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun renderS() {
            sessBox.removeAllViews()
            sess.forEachIndexed { i, s ->
                val wrap = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = round(if (i == active) Color.parseColor("#4F5890") else CARD, 12)
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                }
                val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                r.addView(sp(Data.SHORT[s.day]) {
                    AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setItems(Data.DAYS) { _, w -> s.day = w; active = i; renderS() }.show()
                }, lp(0, -2, wt = 1.1f))
                r.addView(sp(Data.fmt(s.st)) { pickTime(s.st) { v -> s.st = v; active = i; renderS() } }, lp(0, -2, l = 6, wt = 1f))
                r.addView(TextView(this).apply { text = "–"; setTextColor(MUT); gravity = Gravity.CENTER }, lp(dp(18), -2))
                r.addView(sp(Data.fmt(s.en)) { pickTime(s.en) { v -> s.en = v; active = i; renderS() } }, lp(0, -2, wt = 1f))
                r.addView(sp("✕") {
                    if (sess.size > 1) { sess.removeAt(i); active = minOf(active, sess.size - 1); renderS() }
                    else toast("Debe haber polo menos unha sesión")
                }, lp(dp(40), -2, l = 6))
                wrap.addView(r)
                wrap.addView(EditText(this).apply {
                    hint = "Aula desta sesión (opcional)"; setSingleLine(); textSize = 14f; setText(s.room)
                    addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(e: Editable?) { s.room = e?.toString() ?: "" }
                        override fun beforeTextChanged(a: CharSequence?, st: Int, cn: Int, af: Int) {}
                        override fun onTextChanged(a: CharSequence?, st: Int, bf: Int, cn: Int) {}
                    })
                    setOnFocusChangeListener { _, f -> if (f) active = i }
                })
                sessBox.addView(wrap, lp(-1, -2, b = 8))
            }
        }

        if (used.isNotEmpty()) {
            label("Materias que xa usaches (toca para copiar)", 0)
            box.addView(hrow(used.map { (n, d) -> chip("${Data.GLYPHS[Data.shape(n)]} " + (if (d.isEmpty()) n else "$n · $d"), Data.color(n)) {
                name.setText(n); det.setText(d); name.setSelection(n.length)
                other = items.firstOrNull { x -> x.name == n && x.detail == d }?.other ?: false
                picked = Data.shape(n); refresh() } }))
        }
        label("Materia", if (used.isNotEmpty()) 12 else 0)
        box.addView(name); box.addView(det)
        label("Tipo de materia")
        val tClase = TextView(this); val tOutra = TextView(this)
        fun paintT() {
            tClase.setTextColor(if (!other) DARK else INK); tClase.background = round(if (!other) INK else CARD, 12)
            tOutra.setTextColor(if (other) DARK else INK)
            tOutra.background = if (other) StripeDrawable(INK, Color.argb(55, 29, 33, 55), dp(12).toFloat(), dp(10).toFloat(), dp(3).toFloat())
                                else StripeDrawable(CARD, Color.argb(45, 255, 255, 255), dp(12).toFloat(), dp(10).toFloat(), dp(3).toFloat())
        }
        listOf(tClase to "Clase", tOutra to "Outra (raiada)").forEach { (t, s) ->
            t.text = s; t.gravity = Gravity.CENTER; t.textSize = 14f; t.typeface = Typeface.DEFAULT_BOLD
            t.setPadding(0, dp(11), 0, dp(11))
        }
        tClase.setOnClickListener { other = false; paintT() }
        tOutra.setOnClickListener { other = true; paintT() }
        val tRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tRow.addView(tClase, lp(0, -2, wt = 1f)); tRow.addView(tOutra, lp(0, -2, l = 6, wt = 1f))
        box.addView(tRow)
        label("Cor da materia (se non elixes, asígnase soa)")
        val swatches = mutableListOf<TextView>()
        fun paintSw() = swatches.forEachIndexed { i, t ->
            t.background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat(); setColor(Data.paletteColor(i))
                if (picked == i) setStroke(dp(3), INK) else setStroke(dp(1), Color.parseColor("#55FFFFFF"))
            }
        }
        for (rowN in 0..1) {
            val lr = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (i in rowN * 6 until rowN * 6 + 6) {
                val t = TextView(this).apply {
                    text = Data.GLYPHS[i]; gravity = Gravity.CENTER; textSize = 16f
                    setTextColor(Data.textOn(Data.paletteColor(i))); setOnClickListener { picked = i; paintSw() }
                }
                swatches.add(t); lr.addView(t, lp(0, dp(40), l = if (i % 6 == 0) 0 else 6, wt = 1f))
            }
            box.addView(lr, lp(-1, -2, b = 6))
        }
        paintSw()
        refresh = { paintT(); paintSw() }; refresh()
        label("Sesións: día, hora e aula (cada sesión pode ter a súa)")
        box.addView(sessBox)
        box.addView(TextView(this).apply {
            text = "+  Outro día ou hora"; textSize = 14f; setTextColor(INK); gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
            background = round(CARD, 12); setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                val l = sess.last()
                sess.add(Sess(minOf(l.day + 1, 4), l.st, l.en, l.room)); active = sess.size - 1; renderS()
            }
        })
        if (slots.isNotEmpty()) {
            label("Franxas que xa usaches (aplícanse á sesión marcada)")
            box.addView(hrow(slots.map { (a2, b2) -> chip("${Data.fmt(a2)}–${Data.fmt(b2)}", Color.parseColor("#C9CEE8")) {
                sess[active].st = a2; sess[active].en = b2; renderS() } }))
        }
        renderS()

        val b = AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(if (old == null) "Nova materia" else "Editar materia")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Gardar", null).setNegativeButton("Cancelar", null)
        if (idx != null) b.setNeutralButton("Eliminar materia") { _, _ -> items.removeAll(group.toSet()); commit() }
        val d = b.create(); d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nm = name.text.toString().trim().replace(";", ",")
            val bad = sess.firstOrNull { it.en <= it.st }
            when {
                nm.isEmpty() -> toast("Pon un nome á materia")
                bad != null -> toast("${Data.DAYS[bad.day]}: a hora de fin debe ser despois da de inicio")
                else -> {
                    val dt = det.text.toString().trim().replace(";", ",")
                    items.removeAll(group.toSet())
                    sess.forEach { s -> items.add(Item(s.day, s.st, s.en, nm, dt, s.room.trim().replace(";", ","), other)) }
                    items = items.distinct().toMutableList()
                    picked?.let { Data.setColor(this, nm, it) }
                    commit(); d.dismiss()
                }
            }
        }
    }
}
