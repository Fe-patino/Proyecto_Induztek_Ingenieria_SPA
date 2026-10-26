package cl.duocuc.miregistro.model

data class Usuario(
    val nombre: String,
    val correo: String,

    val ciudad: String = "",

    val id: Long = 0,

    val rolId: Long = Rol.ID_TECNICO,

    val rolNombre: String = "",

    val contrasena: String = "",

    val avatar: ByteArray? = null

)
