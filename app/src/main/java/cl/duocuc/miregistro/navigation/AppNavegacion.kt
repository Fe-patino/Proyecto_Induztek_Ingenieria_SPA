package cl.duocuc.miregistro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.miregistro.model.Rol
import cl.duocuc.miregistro.ui.screens.DetalleProtocoloScreen
import cl.duocuc.miregistro.ui.screens.HistorialEquipoScreen
import cl.duocuc.miregistro.ui.screens.InicioScreen
import cl.duocuc.miregistro.ui.screens.LoginScreen
import cl.duocuc.miregistro.ui.screens.NuevoProtocoloScreen
import cl.duocuc.miregistro.ui.screens.PerfilScreen
import cl.duocuc.miregistro.viewmodel.ProtoFieldViewModel

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    // NUEVO (ViewModel compartido): UN solo ProtoFieldViewModel para todas las pantallas.
    // Es un AndroidViewModel: al crearse abre la BD y lee la sesión guardada.
    val viewModel: ProtoFieldViewModel = viewModel()
    val usuarioActual by viewModel.usuarioActual.collectAsStateWithLifecycle()
    // NUEVO (Inicio): lista, contadores y filtros. Todos salen de la BD (fuente de verdad).
    val protocolos by viewModel.protocolos.collectAsStateWithLifecycle()
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val filtro by viewModel.filtro.collectAsStateWithLifecycle()
    val estadoFiltro by viewModel.estadoFiltro.collectAsStateWithLifecycle()

    // NUEVO (Sesión): si ya hay sesión guardada la app abre en Inicio; si no, en Login.
    // remember { }: se decide UNA vez al abrir la app. Si cambiara en cada recomposición,
    // el NavHost se reconstruiría al iniciar o cerrar sesión.
    val destinoInicial = remember {
        if (viewModel.usuarioActual.value != null) Ruta.Inicio.ruta else Ruta.Login.ruta
    }

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(Ruta.Login.ruta) {
            LoginScreen(
                onIniciarSesion = { correo, contrasena ->
                    val correcto = viewModel.iniciarSesion(correo, contrasena)
                    if (correcto) {
                        // NUEVO (Sesión): popUpTo saca el Login de la pila: con "Atrás"
                        // no se vuelve al Login estando con la sesión iniciada
                        navController.navigate(Ruta.Inicio.ruta) {
                            popUpTo(Ruta.Login.ruta) { inclusive = true }
                        }
                    }
                    correcto
                }
            )
        }

        composable(Ruta.Inicio.ruta) {
            usuarioActual?.let { usuario ->
                InicioScreen(
                    usuario = usuario,
                    resumen = resumen,
                    protocolos = protocolos,
                    filtro = filtro,
                    estadoFiltro = estadoFiltro,
                    onBuscar = viewModel::buscar,
                    onFiltrarEstado = viewModel::filtrarPorEstado,
                    onNuevoProtocolo = { navController.navigate(Ruta.NuevoProtocolo.ruta) },
                    // NUEVO (Detalle): se navega con el id del protocolo tocado
                    onVerProtocolo = { id -> navController.navigate(Ruta.Detalle.crear(id)) },
                    onVerPerfil = { navController.navigate(Ruta.Perfil.ruta) },
                    onCerrarSesion = {
                        viewModel.cerrarSesion()
                        // NUEVO (Sesión): se vuelve al Login y se borra todo lo anterior de la pila
                        navController.navigate(Ruta.Login.ruta) {
                            popUpTo(Ruta.Inicio.ruta) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Ruta.NuevoProtocolo.ruta) {
            usuarioActual?.let { usuario ->
                NuevoProtocoloScreen(
                    equipos = viewModel.equipos,
                    usuarioId = usuario.id,
                    onVolver = { navController.navigateUp() },
                    onGuardar = { protocolo ->
                        // NUEVO (SQLite): INSERT del protocolo y sus mediciones, y se recarga la lista.
                        val id = viewModel.guardarProtocolo(protocolo)
                        if (id != -1L) {
                            // NUEVO (Detalle): se muestra el comprobante del protocolo nuevo.
                            // popUpTo saca el formulario de la pila: "Atrás" vuelve al Inicio.
                            navController.navigate(Ruta.Detalle.crear(id)) {
                                popUpTo(Ruta.NuevoProtocolo.ruta) { inclusive = true }
                            }
                        } else {
                            navController.navigateUp()
                        }
                    }
                )
            }
        }

        composable(
            route = Ruta.Detalle.ruta,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entrada ->
            val id = entrada.arguments?.getLong("id") ?: 0L
            // NUEVO (Detalle): el protocolo se lee de la BD por su id. remember(id) -> una vez por id.
            // mutableStateOf: al formalizar se vuelve a leer de la BD y la pantalla se redibuja.
            var protocolo by remember(id) { mutableStateOf(viewModel.obtenerProtocolo(id)) }
            protocolo?.let { actual ->
                DetalleProtocoloScreen(
                    protocolo = actual,
                    esSupervisor = usuarioActual?.rolId == Rol.ID_SUPERVISOR,
                    onVolver = { navController.navigateUp() },
                    onFormalizar = {
                        viewModel.formalizarProtocolo(id)
                        protocolo = viewModel.obtenerProtocolo(id)
                    },
                    // NUEVO (Historial): se navega con el id del EQUIPO de este protocolo
                    onVerHistorial = { navController.navigate(Ruta.Historial.crear(actual.equipoId)) }
                )
            }
        }

        composable(
            route = Ruta.Historial.ruta,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entrada ->
            val idEquipo = entrada.arguments?.getLong("id") ?: 0L
            // NUEVO (Historial): se consulta la BD una vez por equipo (remember(id))
            val equipo = remember(idEquipo) { viewModel.obtenerEquipo(idEquipo) }
            val pruebas = remember(idEquipo) { viewModel.historialEquipo(idEquipo) }
            if (equipo != null) {
                HistorialEquipoScreen(
                    equipo = equipo,
                    protocolos = pruebas,
                    onVolver = { navController.navigateUp() }
                )
            }
        }

        composable(Ruta.Perfil.ruta) {
            usuarioActual?.let { usuario ->
                PerfilScreen(
                    usuario = usuario,
                    // NUEVO (Perfil): los contadores del ViewModel ya son los del usuario actual
                    resumen = resumen,
                    onFotoTomada = viewModel::actualizarAvatar,
                    onImagenElegida = viewModel::actualizarAvatar,
                    onCerrarSesion = {
                        viewModel.cerrarSesion()
                        // popUpTo(Inicio) inclusive también saca el Perfil de la pila
                        navController.navigate(Ruta.Login.ruta) {
                            popUpTo(Ruta.Inicio.ruta) { inclusive = true }
                        }
                    },
                    onVolver = { navController.navigateUp() }
                )
            }
        }
    }
}
