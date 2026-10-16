package cl.duocuc.miregistro.navigation

sealed class Ruta(val ruta: String) {
    data object Formulario : Ruta("formulario")
    data object Perfil : Ruta("perfil")
    data object Usuarios : Ruta("usuarios")

    // NUEVO (SQLite): la ruta lleva el ID (clave primaria), ya no la posición en la lista.
    // Con el buscador la lista se filtra y las posiciones cambian; el id nunca cambia.
    data object Editar : Ruta("editar/{id}") {
        fun crear(id: Long) = "editar/$id"
    }
}
