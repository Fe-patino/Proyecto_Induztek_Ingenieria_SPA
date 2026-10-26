package cl.duocuc.miregistro.model

import androidx.annotation.StringRes


data class
LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val contrasenaVisible: Boolean = false,

    // NUEVO (Login): null = sin error
    @StringRes val errorCorreo: Int? = null,
    @StringRes val errorContrasena: Int? = null,
    // NUEVO (Login): el formato es válido, pero el correo o la contraseña no existen en la BD
    @StringRes val errorCredenciales: Int? = null
)
