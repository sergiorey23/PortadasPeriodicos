package sergirex.portadasperiodicos

import java.io.Serializable

data class Portada(
    val periodico: String,
    val title: String,
    var fecha: String,
    val webPeriodico: String,
    val siglaPais: String
) : Serializable
