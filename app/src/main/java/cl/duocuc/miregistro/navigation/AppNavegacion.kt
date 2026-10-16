package cl.duocuc.miregistro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duocuc.miregistro.ui.screens.EditarScreen
import cl.duocuc.miregistro.ui.screens.FormularioScreen
import cl.duocuc.miregistro.ui.screens.PerfilScreen
import cl.duocuc.miregistro.ui.screens.UsuariosScreen
import cl.duocuc.miregistro.viewmodel.UsuariosViewModel

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    // NUEVO (SQLite + SharedPreferences): la línea no cambia, pero ahora el ViewModel
    // es un AndroidViewModel: al crearse lee la BD y la sesión guardada.
    val usuariosViewModel: UsuariosViewModel = viewModel()
    val usuarioActual by usuariosViewModel.usuarioActual.collectAsStateWithLifecycle()
    val usuarios by usuariosViewModel.usuarios.collectAsStateWithLifecycle()
    // NUEVO (Buscar): texto del buscador
    val filtro by usuariosViewModel.filtro.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Ruta.Formulario.ruta) {
        composable(Ruta.Formulario.ruta) {
            FormularioScreen(
                onRegistroExitoso = { usuario ->
                    usuariosViewModel.registrar(usuario)
                    navController.navigate(Ruta.Perfil.ruta)
                },
                usuarioActual = usuarioActual,
                onVerMiPerfil = { navController.navigate(Ruta.Perfil.ruta) },
                onVerUsuarios = { navController.navigate(Ruta.Usuarios.ruta) },
                onCerrarSesion = { usuariosViewModel.cerrarSesion() }
            )
        }

        composable(Ruta.Perfil.ruta) {
            PerfilScreen(
                usuario = usuarioActual,
                onVolver = { navController.navigateUp() }
            )
        }

        composable(Ruta.Usuarios.ruta) {
            UsuariosScreen(
                usuarios = usuarios,
                onVolver = { navController.navigateUp() },
                // NUEVO (SQLite): se navega con el id del usuario tocado
                onEditar = { id -> navController.navigate(Ruta.Editar.crear(id)) },
                filtro = filtro,                              // NUEVO (Buscar)
                onBuscar = usuariosViewModel::buscar,         // NUEVO (Buscar)
                onEliminar = usuariosViewModel::eliminar      // NUEVO (Eliminar)
            )
        }

        composable(
            route = Ruta.Editar.ruta,
            // NUEVO (SQLite): el argumento ahora es "id" de tipo Long
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entrada ->
            val id = entrada.arguments?.getLong("id") ?: 0L
            // NUEVO (SQLite): se busca el usuario en la BD por su id.
            // remember(id) -> la consulta se hace UNA vez, no en cada recomposición.
            val usuario = remember(id) { usuariosViewModel.obtener(id) }
            if (usuario != null) {
                EditarScreen(
                    usuario = usuario,
                    roles = usuariosViewModel.roles,          // NUEVO (Rol): opciones del desplegable
                    onVolver = { navController.navigateUp() },
                    onGuardar = { editado ->
                        usuariosViewModel.actualizar(editado) // NUEVO (SQLite): UPDATE por id
                        navController.navigateUp()
                    }
                )
            }
        }
    }
}
