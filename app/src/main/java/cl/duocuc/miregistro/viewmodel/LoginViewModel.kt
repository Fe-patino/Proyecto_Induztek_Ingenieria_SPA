package cl.duocuc.miregistro.viewmodel

import androidx.lifecycle.ViewModel
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.LoginUiState
import cl.duocuc.miregistro.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// =====================================================================
//  NUEVO (Login) — LoginViewModel.kt
//  ---------------------------------------------------------------------
//  ViewModel SIMPLE (no toca la BD): solo guarda lo que el técnico escribe y
//  VALIDA el formato. Preguntar a la BD si las credenciales existen es trabajo
//  del ProtoFieldViewModel compartido; esa respuesta vuelve aquí con
//  onCredencialesInvalidas().
// =====================================================================

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // NUEVO (Login): al escribir se borran los errores para no mostrar mensajes viejos
    fun onCorreoCambia(valor: String) {
        _uiState.update { it.copy(correo = valor.trim(), errorCorreo = null, errorCredenciales = null) }
    }

    fun onContrasenaCambia(valor: String) {
        _uiState.update { it.copy(contrasena = valor, errorContrasena = null, errorCredenciales = null) }
    }

    fun onAlternarVisibilidad() {
        _uiState.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }
    }

    // NUEVO (Login): valida el formato. Devuelve las credenciales listas para buscar en la BD
    // (en un Usuario con solo correo y contraseña) o null si hay errores.
    fun onIngresar(): Usuario? {
        val estado = _uiState.value

        val errorCorreo = when {
            estado.correo.isBlank() -> R.string.error_correo_obligatorio
            !estado.correo.contains("@") || !estado.correo.contains(".") -> R.string.error_correo_invalido
            else -> null
        }
        val errorContrasena = if (estado.contrasena.isEmpty()) R.string.error_contrasena_obligatoria else null

        _uiState.update { it.copy(errorCorreo = errorCorreo, errorContrasena = errorContrasena) }

        return if (errorCorreo != null || errorContrasena != null) null
        else Usuario(nombre = "", correo = estado.correo, contrasena = estado.contrasena)
    }

    // NUEVO (Login): la BD respondió que no existe un usuario con ese correo y contraseña
    fun onCredencialesInvalidas() {
        _uiState.update { it.copy(errorCredenciales = R.string.error_credenciales) }
    }
}
