package cl.duocuc.miregistro.viewmodel

import androidx.lifecycle.ViewModel
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.Equipo
import cl.duocuc.miregistro.model.EstadoProtocolo
import cl.duocuc.miregistro.model.NuevoProtocoloUiState
import cl.duocuc.miregistro.model.Protocolo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// =====================================================================
//  NUEVO (Nuevo protocolo) — NuevoProtocoloViewModel.kt
//  ---------------------------------------------------------------------
//  ViewModel SIMPLE (no toca la BD), como FormularioViewModel: guarda lo que el
//  técnico escribe, controla el paso actual y VALIDA. Al final devuelve el
//  Protocolo armado (o null); guardarlo en SQLite es trabajo del ProtoFieldViewModel.
//  Se crea dentro de la ruta, por eso cada vez que se entra el formulario parte vacío.
// =====================================================================

class NuevoProtocoloViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NuevoProtocoloUiState())
    val uiState: StateFlow<NuevoProtocoloUiState> = _uiState.asStateFlow()

    // NUEVO (Paso 1): al elegir otro equipo cambian los parámetros -> se vacían los valores.
    // Si se elige el mismo equipo otra vez, no se pierde lo escrito.
    fun onEquipoSeleccionado(equipo: Equipo) {
        _uiState.update {
            if (it.equipo?.id == equipo.id) it
            else it.copy(
                equipo = equipo,
                valores = List(equipo.tipo.parametros.size) { "" },
                erroresMediciones = emptyList(),
                errorEquipo = null
            )
        }
    }

    // NUEVO (Paso 2): solo deja pasar dígitos, punto, coma y signo menos
    fun onValorCambia(indice: Int, texto: String) {
        val limpio = texto.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        _uiState.update { estado ->
            if (indice !in estado.valores.indices) estado
            else estado.copy(
                valores = estado.valores.toMutableList().also { it[indice] = limpio },
                erroresMediciones = estado.erroresMediciones.toMutableList().also {
                    if (indice in it.indices) it[indice] = null
                }
            )
        }
    }

    fun onResultadoCambia(resultado: String) {
        _uiState.update { it.copy(resultado = resultado) }
    }

    fun onObservacionesCambia(texto: String) {
        _uiState.update { it.copy(observaciones = texto) }
    }

    fun onAnterior() {
        _uiState.update { it.copy(paso = (it.paso - 1).coerceAtLeast(1)) }
    }

    // NUEVO (Pasos): valida SOLO el paso actual; si está bien, avanza al siguiente.
    // Un valor fuera de rango NO bloquea: solo un valor vacío o que no es número.
    fun onSiguiente() {
        val estado = _uiState.value
        when (estado.paso) {
            1 -> {
                if (estado.equipo == null) {
                    _uiState.update { it.copy(errorEquipo = R.string.error_equipo_obligatorio) }
                } else {
                    _uiState.update { it.copy(paso = 2) }
                }
            }
            2 -> {
                val errores = estado.valores.indices.map { indice ->
                    when {
                        estado.valores[indice].isBlank() -> R.string.error_medicion_obligatoria
                        estado.valorNumerico(indice) == null -> R.string.error_medicion_invalida
                        else -> null
                    }
                }
                _uiState.update {
                    it.copy(
                        erroresMediciones = errores,
                        paso = if (errores.all { error -> error == null }) 3 else it.paso
                    )
                }
            }
        }
    }

    // NUEVO (Guardar): arma el Protocolo. La fecha y hora se toman AQUÍ, al guardar, y el
    // estado siempre parte como EN_TERRENO (el Supervisor lo formaliza después).
    fun onGuardar(usuarioId: Long): Protocolo? {
        val estado = _uiState.value
        val equipo = estado.equipo ?: return null
        val mediciones = estado.mediciones()
        // Seguridad: deben estar TODAS las mediciones del tipo de equipo
        if (mediciones.size != equipo.tipo.parametros.size) return null

        return Protocolo(
            equipoId = equipo.id,
            usuarioId = usuarioId,
            fechaHora = System.currentTimeMillis(),
            resultado = estado.resultado,
            estado = EstadoProtocolo.EN_TERRENO,
            observaciones = estado.observaciones.trim(),
            mediciones = mediciones
        )
    }
}
