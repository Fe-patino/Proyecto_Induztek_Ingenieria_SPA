package cl.duocuc.miregistro.model


fun Double.aTexto(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

data class Medicion(
    val parametro: String,
    val valor: Double,
    val unidad: String,
    val minimo: Double,
    val maximo: Double,
    // NUEVO (SQLite): claves de la fila. 0 = "aún no guardada en la BD".
    val id: Long = 0,
    val protocoloId: Long = 0
) {
    // NUEVO (Medición): regla del caso -> fuera de rango si pasa el mínimo o el máximo
    val fueraDeRango: Boolean
        get() = valor < minimo || valor > maximo
}


