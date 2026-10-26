package cl.duocuc.miregistro.model


data class ParametroMedicion(
    val nombre: String,
    val unidad: String,
    val minimo: Double,
    val maximo: Double
)


enum class TipoEquipo(val etiqueta: String, val parametros: List<ParametroMedicion>) {
    TRANSFORMADOR(
        "Transformador",
        listOf(
            ParametroMedicion("Resistencia de aislamiento", "MΩ", 1000.0, 5000.0),
            ParametroMedicion("Relación de transformación", "V/V", 19.8, 20.2)
        )
    ),
    RELE(
        "Relé de protección",
        listOf(
            ParametroMedicion("Tiempo de operación", "ms", 20.0, 50.0),
            ParametroMedicion("Corriente de pickup", "A", 4.8, 5.2)
        )
    ),
    CELDA(
        "Celda de media tensión",
        listOf(
            ParametroMedicion("Resistencia de contacto", "µΩ", 10.0, 80.0),
            ParametroMedicion("Resistencia de aislamiento", "MΩ", 500.0, 5000.0)
        )
    ),
    CABLE(
        "Cable de poder",
        listOf(
            ParametroMedicion("Resistencia de aislamiento", "MΩ", 1000.0, 10000.0)
        )
    )
}

data class Equipo(
    val id: Long,
    val codigo: String,
    val tipo: TipoEquipo,
    val marca: String,
    // NUEVO (Equipo): la instalación llega gracias al JOIN con la tabla instalaciones
    val instalacion: Instalacion
)
