package cl.duocuc.miregistro.model

data class Usuario(
    val nombre: String,
    val correo: String,
    val ciudad: String,
    // NUEVO (SQLite): clave primaria de la fila. 0 = "aún no guardado en la BD".
    val id: Long = 0,
    // NUEVO (Rol): clave foránea -> qué rol tiene. Por defecto, Estudiante.
    val rolId: Long = Rol.ID_ESTUDIANTE,
    // NUEVO (Rol): nombre del rol. NO es columna de "usuarios": llega gracias al JOIN.
    val rolNombre: String = ""
    // Los nuevos campos van al final y con valor por defecto:
    // Usuario(nombre, correo, ciudad) sigue funcionando en el Formulario.
)
