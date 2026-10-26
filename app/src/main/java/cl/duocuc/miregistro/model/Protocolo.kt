package cl.duocuc.miregistro.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


enum class EstadoProtocolo(val etiqueta: String) {
    EN_TERRENO("Registrado en terreno"),
    FORMALIZADO("Formalizado")
}

data class Protocolo(
    val equipoId: Long,
    val usuarioId: Long,
    // NUEVO (Protocolo): fecha y hora en milisegundos (System.currentTimeMillis())
    val fechaHora: Long,
    val resultado: String,
    val estado: EstadoProtocolo = EstadoProtocolo.EN_TERRENO,
    val observaciones: String = "",
    // NUEVO (SQLite): clave primaria de la fila. 0 = "aún no guardado en la BD".
    val id: Long = 0,
    // NUEVO (Protocolo): datos que llegan por JOIN (no son columnas de "protocolos")
    val equipoCodigo: String = "",
    val equipoTipo: TipoEquipo = TipoEquipo.TRANSFORMADOR,
    val planta: String = "",
    val cliente: String = "",
    val tecnicoNombre: String = "",
    // NUEVO (Protocolo): cuántas mediciones quedaron fuera de rango (lo calcula la consulta)
    val medicionesFueraDeRango: Int = 0,
    // NUEVO (Protocolo): se llena en obtenerProtocolo / historial, no en la lista del Inicio
    val mediciones: List<Medicion> = emptyList()
) {
    // NUEVO (Protocolo): la fecha en milisegundos, escrita para personas (ej. "23/10/2026 14:35").
    // La usan Inicio, Detalle e Historial, así el formato es el mismo en toda la app.
    val fechaTexto: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fechaHora))

    companion object {
        // NUEVO (Protocolo): valores permitidos de la columna "resultado"
        const val RESULTADO_APROBADO = "Aprobado"
        const val RESULTADO_RECHAZADO = "Rechazado"
    }
}

// NUEVO (Inicio): los 4 números de las tarjetas estadísticas
data class ResumenProtocolos(
    val total: Int = 0,
    val enTerreno: Int = 0,
    val formalizados: Int = 0,
    val fueraDeRango: Int = 0
)
