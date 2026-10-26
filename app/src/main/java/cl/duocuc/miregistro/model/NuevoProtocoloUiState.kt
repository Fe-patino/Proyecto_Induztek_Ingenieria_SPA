package cl.duocuc.miregistro.model

import androidx.annotation.StringRes



data class NuevoProtocoloUiState(
    // NUEVO (Pasos): 1, 2 o 3
    val paso: Int = 1,
    val equipo: Equipo? = null,
    // NUEVO (Mediciones): un texto por cada parámetro del tipo de equipo, en el mismo orden
    val valores: List<String> = emptyList(),
    val resultado: String = Protocolo.RESULTADO_APROBADO,
    val observaciones: String = "",

    // NUEVO (Validación): ids de strings.xml; null = sin error
    @StringRes val errorEquipo: Int? = null,
    val erroresMediciones: List<Int?> = emptyList()
) {
    companion object {
        const val TOTAL_PASOS = 3
    }

    // NUEVO (Mediciones): texto -> número. Acepta coma decimal. null si está vacío o no es número.
    fun valorNumerico(indice: Int): Double? =
        valores.getOrNull(indice)?.trim()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it.isFinite() }

    // NUEVO (Mediciones): ¿el valor escrito está fuera del rango de su parámetro?
    // Es solo un AVISO: una medición fuera de rango es un dato válido (el equipo puede estar mal).
    fun fueraDeRango(indice: Int): Boolean {
        val parametro = equipo?.tipo?.parametros?.getOrNull(indice) ?: return false
        val valor = valorNumerico(indice) ?: return false
        return valor < parametro.minimo || valor > parametro.maximo
    }

    // NUEVO (Mediciones): arma las Medicion con los valores ya escritos (para el resumen y para guardar)
    fun mediciones(): List<Medicion> =
        equipo?.tipo?.parametros.orEmpty().mapIndexedNotNull { indice, parametro ->
            valorNumerico(indice)?.let { valor ->
                Medicion(
                    parametro = parametro.nombre,
                    valor = valor,
                    unidad = parametro.unidad,
                    minimo = parametro.minimo,
                    maximo = parametro.maximo
                )
            }
        }
}
