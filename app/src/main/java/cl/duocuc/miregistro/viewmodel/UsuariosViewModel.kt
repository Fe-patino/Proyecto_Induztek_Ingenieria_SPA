package cl.duocuc.miregistro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.miregistro.data.SesionPreferencias
import cl.duocuc.miregistro.data.UsuarioDbHelper
import cl.duocuc.miregistro.model.Rol
import cl.duocuc.miregistro.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// NUEVO (SQLite + SharedPreferences): ViewModel -> AndroidViewModel.
// La BD y las preferencias necesitan un Context; AndroidViewModel recibe la Application.
// viewModel() en AppNavegacion lo sigue creando igual: no hace falta un Factory.
class UsuariosViewModel(application: Application) : AndroidViewModel(application) {

    // NUEVO (SQLite): acceso a las tablas roles y usuarios
    private val db = UsuarioDbHelper(application)

    // NUEVO (SharedPreferences): acceso a la sesión guardada
    private val sesion = SesionPreferencias(application)

    // NUEVO (Buscar): texto escrito en el buscador ("" = sin filtro)
    private val _filtro = MutableStateFlow("")
    val filtro: StateFlow<String> = _filtro.asStateFlow()

    // NUEVO (SQLite): la lista se CARGA desde la BD (ya no está escrita a mano)
    private val _usuarios = MutableStateFlow(db.listarUsuarios())
    val usuarios: StateFlow<List<Usuario>> = _usuarios.asStateFlow()

    // NUEVO (Rol): los roles no cambian mientras la app corre -> se leen una sola vez
    val roles: List<Rol> = db.listarRoles()

    // NUEVO (SharedPreferences + SQLite): id guardado -> se busca ese usuario en la BD
    private val _usuarioActual = MutableStateFlow(db.buscarPorId(sesion.leer()))
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    // NUEVO (SQLite): vuelve a leer la tabla respetando el filtro actual.
    // Se llama después de CADA cambio: la BD es la "fuente de verdad".
    private fun recargar() {
        val texto = _filtro.value.trim()
        _usuarios.value = if (texto.isEmpty()) db.listarUsuarios() else db.buscarUsuarios(texto)
    }

    // NUEVO (Buscar): cada letra escrita en el buscador llega aquí
    fun buscar(texto: String) {
        _filtro.value = texto
        recargar()
    }

    // NUEVO (SQLite): para la pantalla Editar, que recibe solo el id en la ruta
    fun obtener(id: Long): Usuario? = db.buscarPorId(id)

    fun registrar(usuario: Usuario) {
        // NUEVO (SQLite): INSERT y se relee con JOIN para traer también el nombre del rol
        val id = db.insertarUsuario(usuario)
        _usuarioActual.value = db.buscarPorId(id)
        // NUEVO (SharedPreferences): se recuerda quién inició sesión
        sesion.guardar(id)
        recargar()
    }

    // NUEVO (SQLite): ahora recibe el usuario completo (con su id), ya no un índice
    fun actualizar(editado: Usuario) {
        db.actualizarUsuario(editado)
        // Si me edité a mí mismo, la sesión también muestra los datos (y el rol) nuevos
        if (_usuarioActual.value?.id == editado.id) _usuarioActual.value = db.buscarPorId(editado.id)
        recargar()
    }

    // NUEVO (Eliminar): DELETE en la BD. Si borro al usuario con sesión, se cierra la sesión.
    fun eliminar(usuario: Usuario) {
        db.eliminarUsuario(usuario.id)
        if (_usuarioActual.value?.id == usuario.id) cerrarSesion()
        recargar()
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        // NUEVO (SharedPreferences): se borra la sesión guardada
        sesion.borrar()
    }

    // NUEVO (SQLite): al destruirse el ViewModel se cierra la conexión
    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}
