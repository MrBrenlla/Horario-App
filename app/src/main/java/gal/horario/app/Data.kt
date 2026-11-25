package gal.horario.app

import android.content.Context
import android.graphics.Color

data class Item(val day: Int, val start: Int, val end: Int, val name: String, val detail: String)

object Data {
    val DAYS = arrayOf("Luns", "Martes", "Mércores", "Xoves", "Venres")
    val SHORT = arrayOf("Lun", "Mar", "Mér", "Xov", "Ven")
    const val S = 505   // 8:25
    const val E = 845   // 14:05
    const val BG = "#0B0C15"

    val DEFAULT = listOf(
        "Luns;9:15;10:05;tec1;1BACB, 1BACC", "Luns;10:25;11:15;tic1;1BACA, 1BACB, 1BACC",
        "Luns;12:25;13:15;ia;4ESOA-E", "Luns;13:15;14:05;tec2;2BACB",
        "Martes;8:25;9:15;tic1;1BACA, 1BACB, 1BACC", "Martes;9:15;10:05;proxectcomp;4ESOC, 4ESOD, 4ESOPDC",
        "Martes;11:15;12:05;tec1;1BACB, 1BACC", "Martes;12:05;12:25;GP;", "Martes;12:25;13:15;XD;",
        "Martes;13:15;14:05;tec2;2BACB",
        "Mércores;9:15;10:05;tec2;2BACB", "Mércores;10:25;11:15;AF;", "Mércores;11:15;12:05;tic1;1BACA, 1BACB, 1BACC",
        "Mércores;12:25;13:15;GARDA;G. aulas", "Mércores;13:15;14:05;ia;4ESO",
        "Xoves;8:25;9:15;tec1;1BACB, 1BACC", "Xoves;9:15;10:05;tec2;2BACB", "Xoves;10:05;10:25;GP;",
        "Xoves;10:25;11:15;XD;", "Xoves;11:15;12:05;tic1;1BACA, 1BACB, 1BACC",
        "Xoves;12:25;13:15;GARDA;G. aulas", "Xoves;13:15;14:05;ia;4ESO",
        "Venres;8:25;9:15;tec1;1BACB, 1BACC", "Venres;9:15;10:05;Reunión;STEAM", "Venres;10:25;11:15;XD;"
    ).joinToString("\n")

    fun load(c: Context): String = c.getSharedPreferences("h", 0).getString("t", DEFAULT) ?: DEFAULT
    fun save(c: Context, t: String) { c.getSharedPreferences("h", 0).edit().putString("t", t).apply() }
    fun items(c: Context): List<Item> = parse(load(c))

    private fun mins(s: String): Int { val p = s.trim().split(":"); return p[0].toInt() * 60 + p[1].toInt() }

    fun parse(t: String): List<Item> = t.lines().mapNotNull { l ->
        try {
            val p = l.split(";").map { it.trim() }
            if (p.size < 4) null else {
                val d = when (p[0].lowercase().take(2)) {
                    "lu" -> 0; "ma" -> 1; "mé", "me" -> 2; "xo" -> 3; "ve" -> 4; else -> -1
                }
                if (d < 0) null else Item(d, mins(p[1]), mins(p[2]), p[3], p.getOrElse(4) { "" })
            }
        } catch (e: Exception) { null }
    }

    fun color(n: String): Int {
        val s = n.lowercase()
        val h = when {
            s.startsWith("tic") -> "#4A78FF"; s.startsWith("tec1") -> "#1FD8A4"
            s.startsWith("tec2") -> "#FF6A2B"; s.startsWith("ia") -> "#A665FF"
            s.startsWith("xd") -> "#FF3D80"; s.startsWith("garda") -> "#FFC93D"
            s == "gp" -> "#D9A441"; else -> "#8D92AA"
        }
        return Color.parseColor(h)
    }
}
