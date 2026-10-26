package cl.duocuc.miregistro.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import cl.duocuc.miregistro.data.ConvertidorImagen
import cl.duocuc.miregistro.data.ProtoFieldDbHelper
import cl.duocuc.miregistro.data.SesionPreferencias
import cl.duocuc.miregistro.model.Equipo
import cl.duocuc.miregistro.model.EstadoProtocolo
import cl.duocuc.miregistro.model.Protocolo
import cl.duocuc.miregistro.model.ResumenProtocolos
import cl.duocuc.miregistro.model.Rol
import cl.duocuc.miregistro.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// =====================================================================
//  NUEVO (ViewModel compartido) — ProtoFieldViewModel.kt
//  ---------------------------------------------------------------------
//  UN solo ViewModel para toda la app, creado en AppNavegacion con viewModel().
//  Es AndroidViewModel porque la BD y las preferencias necesitan un Context;
//  recibe la Application. Regla: la BD es la "fuente de verdad": después de
//  cada cambio se vuelve a leer desde SQLite.
//  Se va completando por etapas: ahora maneja la SESIÓN y la LISTA del Inicio.
// =====================================================================

class ProtoFieldViewModel(application: Application) : AndroidViewModel(application) {

    // NUEVO (SQLite): acceso a las 6 tablas
    private val db = ProtoFieldDbHelper(application)

    // NUEVO (SharedPreferences): acceso a la sesión guardada
    private val sesion = SesionPreferencias(application)

    // NUEVO (SharedPreferences + SQLite): SharedPreferences solo recuerda el ID de quien inició
    // sesión; con ese id se lee el usuario completo (y su rol, por el JOIN) desde la BD.
    // Si hay sesión guardada, la app abre directo en el Inicio.
    private val _usuarioActual = MutableStateFlow(db.buscarUsuarioPorId(sesion.leer()))
    val usuarioActual: StateFlow<Usuario?> = _usuarioActual.asStateFlow()

    // NUEVO (Buscar): texto escrito en el buscador por código de equipo ("" = sin filtro)
    private val _filtro = MutableStateFlow("")
    val filtro: StateFlow<String> = _filtro.asStateFlow()

    // NUEVO (Filtrar): estado elegido en el desplegable. null = "Todos"
    private val _estadoFiltro = MutableStateFlow<EstadoProtocolo?>(null)
    val estadoFiltro: StateFlow<EstadoProtocolo?> = _estadoFiltro.asStateFlow()

    // NUEVO (SQLite): la lista y los contadores se CARGAN desde la BD (se llenan en init)
    private val _protocolos = MutableStateFlow<List<Protocolo>>(emptyList())
    val protocolos: StateFlow<List<Protocolo>> = _protocolos.asStateFlow()

    private val _resumen = MutableStateFlow(ResumenProtocolos())
    val resumen: StateFlow<ResumenProtocolos> = _resumen.asStateFlow()

    // NUEVO (Equipo): los equipos no cambian mientras la app corre -> se leen una sola vez
    // (alimentan el desplegable del formulario Nuevo protocolo)
    val equipos: List<Equipo> = db.listarEquipos()

    init {
        recargar()
    }

    // NUEVO (Rol): el Técnico ve SOLO sus protocolos (devuelve su id); el Supervisor ve
    // todos (devuelve null = sin filtro de usuario en la consulta).
    private fun idUsuarioVisible(): Long? {
        val usuario = _usuarioActual.value
        return if (usuario?.rolId == Rol.ID_TECNICO) usuario.id else null
    }

    // NUEVO (SQLite): vuelve a leer la lista y los contadores respetando el rol y los filtros.
    // Se llama después de CADA cambio: la BD es la "fuente de verdad".
    private fun recargar() {
        if (_usuarioActual.value == null) {
            _protocolos.value = emptyList()
            _resumen.value = ResumenProtocolos()
            return
        }
        val idUsuario = idUsuarioVisible()
        _protocolos.value = db.listarProtocolos(idUsuario, _filtro.value.trim(), _estadoFiltro.value)
        _resumen.value = db.resumen(idUsuario)
    }

    // NUEVO (Buscar): cada letra escrita en el buscador llega aquí
    fun buscar(texto: String) {
        _filtro.value = texto
        recargar()
    }

    // NUEVO (Filtrar): el usuario eligió Todos (null), En terreno o Formalizados
    fun filtrarPorEstado(estado: EstadoProtocolo?) {
        _estadoFiltro.value = estado
        recargar()
    }

    // NUEVO (SQLite): INSERT del protocolo con todas sus mediciones (en una transacción) y se
    // vuelve a leer la lista y los contadores. Devuelve el id generado (o -1 si falla).
    fun guardarProtocolo(protocolo: Protocolo): Long {
        val id = db.insertarProtocolo(protocolo)
        recargar()
        return id
    }

    // NUEVO (Avatar): Bitmap (cámara) -> ByteArray JPEG -> columna BLOB. Después se vuelve a leer
    // el usuario de la BD, así todas las pantallas ven la foto nueva.
    fun actualizarAvatar(foto: Bitmap) {
        val usuario = _usuarioActual.value ?: return
        db.actualizarAvatar(usuario.id, ConvertidorImagen.aBytes(foto))
        _usuarioActual.value = db.buscarUsuarioPorId(usuario.id)
    }

    // NUEVO (Avatar): Uri (galería) -> Bitmap -> mismo camino que la cámara.
    // getApplication() entrega el Context que ConvertidorImagen necesita para leer el Uri.
    fun actualizarAvatar(uri: Uri) {
        ConvertidorImagen.desdeUri(getApplication(), uri)?.let { actualizarAvatar(it) }
    }

    // NUEVO (Detalle): READ de UN protocolo con sus mediciones. La ruta Detalle recibe solo el id.
    fun obtenerProtocolo(id: Long): Protocolo? = db.obtenerProtocolo(id)

    // NUEVO (Historial): el equipo y todas sus pruebas (con mediciones), de la más reciente
    // a la más antigua. La ruta Historial recibe solo el id del equipo.
    fun obtenerEquipo(id: Long): Equipo? = db.buscarEquipoPorId(id)

    fun historialEquipo(idEquipo: Long): List<Protocolo> = db.historialEquipo(idEquipo)

    // NUEVO (Formalizar): UPDATE del estado en la BD y se recargan la lista y los contadores.
    // Solo el Supervisor llega a este botón, pero se valida de nuevo aquí por seguridad.
    fun formalizarProtocolo(id: Long) {
        if (_usuarioActual.value?.rolId != Rol.ID_SUPERVISOR) return
        db.formalizarProtocolo(id)
        recargar()
    }

    // NUEVO (Login): busca el correo y la contraseña en la BD. Si existen, guarda la sesión.
    // Devuelve true si el ingreso fue correcto; el LoginScreen muestra el error si fue false.
    fun iniciarSesion(correo: String, contrasena: String): Boolean {
        val usuario = db.iniciarSesion(correo, contrasena) ?: return false
        _usuarioActual.value = usuario
        // NUEVO (SharedPreferences): se recuerda quién inició sesión
        sesion.guardar(usuario.id)
        // NUEVO (Filtrar): quien entra empieza sin filtros ni búsquedas de otra sesión
        _filtro.value = ""
        _estadoFiltro.value = null
        recargar()
        return true
    }

    fun cerrarSesion() {
        _usuarioActual.value = null
        // NUEVO (SharedPreferences): se borra la sesión guardada
        sesion.borrar()
        recargar()
    }

    // NUEVO (SQLite): al destruirse el ViewModel se cierra la conexión
    override fun onCleared() {
        db.close()
        super.onCleared()
    }
}
