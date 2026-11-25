package gal.horario.app

import android.content.Context
import android.graphics.Color

data class Item(val day: Int, val start: Int, val end: Int, val name: String, val detail: String)

object Data {
    val DAYS = arrayOf("Luns", "Martes", "Mércores", "Xoves", "Venres")
    val SHORT = arrayOf("Lun", "Mar", "Mér", "Xov", "Ven")
    const val BG = "#1D2137"
    private val PALETTE = listOf("#6C8CFF", "#3DD9B0", "#FF8A4C", "#B388FF", "#FF6B9A",
        "#FFCF5C", "#5CC8FF", "#A3E36B", "#FF7A7A", "#D6A8F0")

    /** Rango horario que se debuxa: da primeira clase ao final da última (8:00-15:00 se non hai nada). */
    fun range(l: List<Item>): Pair<Int, Int> {
        if (l.isEmpty()) return 480 to 900
        val s = l.minOf { it.start }
        return s to maxOf(l.maxOf { it.end }, s + 60)
    }

    fun fmt(m: Int) = "%d:%02d".format(m / 60, m % 60)

    fun load(c: Context): String = c.getSharedPreferences("h", 0).getString("t", "") ?: ""
    fun save(c: Context, t: String) { c.getSharedPreferences("h", 0).edit().putString("t", t).apply() }
    fun items(c: Context): List<Item> = parse(load(c))
    fun saveItems(c: Context, l: List<Item>) = save(c, serialize(l))

    /** Agrupa os días que comparten hora, materia e detalle nunha soa liña. */
    fun serialize(l: List<Item>): String =
        l.groupBy { listOf(it.start, it.end, it.name, it.detail) }
            .values.sortedWith(compareBy({ it[0].name.lowercase() }, { it[0].start }))
            .joinToString("\n") { g ->
                val f = g[0]
                "${g.map { it.day }.distinct().sorted().joinToString(",") { Data.DAYS[it] }};${fmt(f.start)};${fmt(f.end)};${f.name};${f.detail}"
            }

    fun exportText(l: List<Item>) =
        "# Horario. Formato: días;inicio;fin;materia;detalle\n# Varios días na mesma liña, separados por comas.\n" + serialize(l) + "\n"

    private fun mins(s: String): Int { val p = s.trim().split(":"); return p[0].trim().toInt() * 60 + p[1].trim().toInt() }
    private fun dayOf(s: String) = when (s.trim().lowercase().take(2)) {
        "lu" -> 0; "ma" -> 1; "mé", "me" -> 2; "xo" -> 3; "ve" -> 4; else -> -1
    }

    fun parse(t: String): List<Item> = t.lines().flatMap { l ->
        try {
            if (l.trim().startsWith("#")) emptyList() else {
                val p = l.split(";").map { it.trim() }
                if (p.size < 4) emptyList() else {
                    val s = mins(p[1]); val e = mins(p[2])
                    val days = p[0].split(",").map { dayOf(it) }
                    if (days.any { it < 0 } || e <= s || p[3].isEmpty()) emptyList()
                    else days.map { Item(it, s, e, p[3], p.getOrElse(4) { "" }) }
                }
            }
        } catch (e: Exception) { emptyList() }
    }.distinct()

    /** Cor estable segundo o nome da materia: o mesmo nome sempre ten a mesma cor. */
    fun color(n: String): Int =
        Color.parseColor(PALETTE[(n.trim().lowercase().hashCode() and 0x7fffffff) % PALETTE.size])
}
