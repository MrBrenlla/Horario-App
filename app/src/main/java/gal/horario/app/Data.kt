package gal.horario.app

import android.content.Context
import android.graphics.Color

data class Item(val day: Int, val start: Int, val end: Int, val name: String, val detail: String, val room: String = "", val other: Boolean = false)

object Data {
    val DAYS = arrayOf("Luns", "Martes", "Mércores", "Xoves", "Venres")
    val SHORT = arrayOf("Lun", "Mar", "Mér", "Xov", "Ven")
    const val BG = "#1D2137"
    /** 12 cores moi separadas en tono e en luminosidade (as primeiras son as máis distintas entre si). */
    private val PALETTE = listOf("#FFD23F", "#2F6BFF", "#F2F2F2", "#FF7A1A", "#00B386", "#7A3FC0",
        "#7FD8FF", "#FF6FA0", "#A0522D", "#8A8F9E", "#E03C31", "#B7E34A")
    val GLYPHS = listOf("●", "■", "▲", "◆", "✚", "★", "▼", "○", "◐", "□", "✕", "►")
    fun paletteColor(i: Int) = Color.parseColor(PALETTE[i])

    private fun rl(c: Int): Double {
        fun f(v: Int): Double { val s = v / 255.0; return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4) }
        return 0.2126 * f(Color.red(c)) + 0.7152 * f(Color.green(c)) + 0.0722 * f(Color.blue(c))
    }
    /** Cor de texto (escuro ou branco) con máis contraste sobre o fondo dado. */
    fun textOn(c: Int): Int {
        val l = rl(c); val dark = rl(Color.parseColor(BG))
        return if ((l + 0.05) / (dark + 0.05) >= 1.05 / (l + 0.05)) Color.parseColor(BG) else Color.WHITE
    }

    private var cmap: Map<String, Int> = emptyMap()
    private fun key(n: String) = n.trim().lowercase()

    /** Rango horario que se debuxa: da primeira clase ao final da última (8:00-15:00 se non hai nada). */
    fun range(l: List<Item>): Pair<Int, Int> {
        if (l.isEmpty()) return 480 to 900
        val s = l.minOf { it.start }
        return s to maxOf(l.maxOf { it.end }, s + 60)
    }

    fun roomLabel(r: String) = when { r.isBlank() -> ""; r.trim()[0].isDigit() -> "Aula ${r.trim()}"; else -> r.trim() }

    fun fmt(m: Int) = "%d:%02d".format(m / 60, m % 60)

    private fun P(c: Context) = c.getSharedPreferences("h", 0)

    /** Primeira vez: o horario que xa había gardado pasa a ser o horario 1. */
    private fun ensure(c: Context) {
        val p = P(c)
        if (p.contains("ids")) return
        p.edit().putString("ids", "1").putString("active", "1").putString("n_1", "Horario 1")
            .putString("t_1", p.getString("t", "")).putString("c_1", p.getString("c", "")).apply()
    }
    private fun idList(c: Context): List<String> { ensure(c); return (P(c).getString("ids", "1") ?: "1").split(",").filter { it.isNotEmpty() } }
    fun activeId(c: Context): String { val l = idList(c); val a = P(c).getString("active", "1") ?: "1"; return if (a in l) a else l.first() }
    fun profiles(c: Context): List<Pair<String, String>> = idList(c).map { it to (P(c).getString("n_$it", "Horario") ?: "Horario") }
    fun activeName(c: Context): String = profiles(c).firstOrNull { it.first == activeId(c) }?.second ?: "Horario"
    fun setActive(c: Context, id: String) { P(c).edit().putString("active", id).apply() }
    fun renameProfile(c: Context, id: String, name: String) { P(c).edit().putString("n_$id", name).apply() }

    fun addProfile(c: Context, name: String, copyFrom: String?): String {
        val l = idList(c); val p = P(c)
        val id = ((l.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: 0) + 1).toString()
        p.edit().putString("ids", (l + id).joinToString(",")).putString("n_$id", name)
            .putString("t_$id", if (copyFrom != null) p.getString("t_$copyFrom", "") else "")
            .putString("c_$id", if (copyFrom != null) p.getString("c_$copyFrom", "") else "").apply()
        return id
    }

    fun deleteProfile(c: Context, id: String) {
        val l = idList(c); if (l.size <= 1) return
        val rest = l - id
        val e = P(c).edit().putString("ids", rest.joinToString(",")).remove("n_$id").remove("t_$id").remove("c_$id")
        if (activeId(c) == id) e.putString("active", rest.first())
        e.apply()
    }

    fun load(c: Context): String { ensure(c); return P(c).getString("t_${activeId(c)}", "") ?: "" }
    fun save(c: Context, t: String) { ensure(c); P(c).edit().putString("t_${activeId(c)}", t).apply() }
    fun items(c: Context): List<Item> { loadColors(c); val l = parse(load(c)); assign(c, l); return l }
    fun saveItems(c: Context, l: List<Item>) { save(c, serialize(l)); loadColors(c); assign(c, l) }

    private fun loadColors(c: Context) {
        cmap = (P(c).getString("c_${activeId(c)}", "") ?: "").lines().mapNotNull {
            val p = it.split("\t"); if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: return@mapNotNull null) else null
        }.toMap()
    }

    /** Cada materia recibe unha cor distinta (e unha forma igual de distinta) e mantense estable. */
    private fun assign(c: Context, l: List<Item>) {
        val names = l.map { key(it.name) }.distinct()
        val out = mutableMapOf<String, Int>(); val used = mutableSetOf<Int>()
        names.forEach { n -> val e = cmap[n]; if (e != null && e in PALETTE.indices && e !in used) { out[n] = e; used.add(e) } }
        names.forEach { n ->
            if (n !in out) {
                val free = PALETTE.indices.firstOrNull { it !in used } ?: PALETTE.indices.minByOrNull { i -> out.values.count { it == i } }!!
                out[n] = free; used.add(free)
            }
        }
        if (out != cmap) {
            cmap = out
            persist(c, out)
        }
    }

    private fun persist(c: Context, m: Map<String, Int>) {
        P(c).edit().putString("c_${activeId(c)}", m.entries.joinToString("\n") { "${it.key}\t${it.value}" }).apply()
    }

    /** Fixa a cor dunha materia. Se outra materia xa a tiña, troca a súa cor pola anterior desta. */
    fun setColor(c: Context, name: String, idx: Int) {
        loadColors(c)
        val m = cmap.toMutableMap(); val k = key(name); val old = m[k]
        val other = m.entries.firstOrNull { it.value == idx && it.key != k }?.key
        if (other != null) { if (old != null) m[other] = old else m.remove(other) }
        m[k] = idx; cmap = m; persist(c, m)
    }

    /** Agrupa os días que comparten hora, materia e detalle nunha soa liña. */
    fun serialize(l: List<Item>): String =
        l.groupBy { listOf(it.start, it.end, it.name, it.detail, it.room, it.other) }
            .values.sortedWith(compareBy({ it[0].name.lowercase() }, { it[0].start }))
            .joinToString("\n") { g ->
                val f = g[0]
                "${g.map { it.day }.distinct().sorted().joinToString(",") { Data.DAYS[it] }};${fmt(f.start)};${fmt(f.end)};${f.name};${f.detail};${f.room}${if (f.other) ";outra" else ""}"
            }

    fun exportText(l: List<Item>) =
        "# Horario. Formato: días;inicio;fin;materia;grupos;aula\n# Varios días na mesma liña, separados por comas. Sétima columna opcional: outra (non é clase, sae raiada).\n" + serialize(l) + "\n"

    private fun mins(s: String): Int { val p = s.trim().split(":"); return p[0].trim().toInt() * 60 + p[1].trim().toInt() }
    private fun dayOf(s: String) = when (s.trim().lowercase().take(2)) {
        "lu" -> 0; "ma" -> 1; "mé", "me" -> 2; "xo" -> 3; "ve" -> 4; else -> -1
    }

    fun parse(t: String): List<Item> = t.lines().flatMap { l ->
        try {
            if (l.trim().startsWith("#")) emptyList<Item>() else {
                val p = l.split(";").map { it.trim() }
                if (p.size < 4) emptyList<Item>() else {
                    val s = mins(p[1]); val e = mins(p[2])
                    val days = p[0].split(",").map { dayOf(it) }
                    if (days.any { it < 0 } || e <= s || p[3].isEmpty()) emptyList<Item>()
                    else days.map { Item(it, s, e, p[3], p.getOrElse(4) { "" }, p.getOrElse(5) { "" }, p.getOrElse(6) { "" }.lowercase().let { v -> v == "outra" || v == "outras" }) }
                }
            }
        } catch (e: Exception) { emptyList<Item>() }
    }.distinct()

    fun shape(n: String): Int = cmap[key(n)] ?: ((key(n).hashCode() and 0x7fffffff) % PALETTE.size)
    fun color(n: String): Int = Color.parseColor(PALETTE[shape(n)])
}
