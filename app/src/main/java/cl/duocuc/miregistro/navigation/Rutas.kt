package cl.duocuc.miregistro.navigation

sealed class Ruta(val ruta: String) {
    // NUEVO (Login): pantalla de ingreso y pantalla principal
    data object Login : Ruta("login")
    data object Inicio : Ruta("inicio")
    data object Perfil : Ruta("perfil")
    // NUEVO (Nuevo protocolo): formulario de 3 pasos (es UNA sola ruta)
    data object NuevoProtocolo : Ruta("nuevo_protocolo")

    // NUEVO (Detalle): la ruta lleva el ID del protocolo (clave primaria), como las demás rutas
    // con argumento: el id no cambia aunque la lista se filtre.
    data object Detalle : Ruta("detalle/{id}") {
        fun crear(id: Long) = "detalle/$id"
    }

    // NUEVO (Historial): aquí el id es el del EQUIPO (no el de un protocolo)
    data object Historial : Ruta("historial/{id}") {
        fun crear(idEquipo: Long) = "historial/$idEquipo"
    }
}
