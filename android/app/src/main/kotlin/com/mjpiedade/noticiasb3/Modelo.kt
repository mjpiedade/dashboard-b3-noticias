package com.mjpiedade.noticiasb3

enum class Sinal(val chave: String, val rotulo: String, val cor: Int) {
    RISK_ON("risk_on", "RISK-ON", 0xFF22C55E.toInt()),
    RISK_OFF("risk_off", "RISK-OFF", 0xFFEF4444.toInt()),
    NEUTRO("neutro", "NEUTRO", 0xFF94A3B8.toInt());

    companion object {
        fun de(chave: String?): Sinal = values().firstOrNull { it.chave == chave } ?: NEUTRO
    }
}

data class Tema(
    val titulo: String,
    val sinal: Sinal,
    val nNoticias: Int,
)

data class Feed(
    val atualizadoEm: String,      // "2026-08-12T14:11" (sem tz — hora de SP)
    val temas: List<Tema>,
) {
    val contagens: Triple<Int, Int, Int>
        get() {
            var on = 0; var neu = 0; var off = 0
            for (t in temas) when (t.sinal) {
                Sinal.RISK_ON -> on++
                Sinal.RISK_OFF -> off++
                Sinal.NEUTRO -> neu++
            }
            return Triple(on, neu, off)
        }

    companion object {
        val Vazio = Feed(atualizadoEm = "", temas = emptyList())
    }
}
