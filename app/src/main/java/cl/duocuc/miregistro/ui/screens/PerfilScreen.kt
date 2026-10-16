package cl.duocuc.miregistro.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.Perfil
import cl.duocuc.miregistro.model.PerfilUiState
import cl.duocuc.miregistro.model.Usuario
import cl.duocuc.miregistro.ui.components.AvatarCircular
import cl.duocuc.miregistro.ui.components.EtiquetaInteres
import cl.duocuc.miregistro.ui.components.TarjetaEstadistica
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.MiRegistroTheme
import cl.duocuc.miregistro.viewmodel.PerfilViewModel

@Composable
fun PerfilScreen(
    usuario: Usuario?,
    onVolver: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val perfil = if (usuario == null) uiState.perfil else uiState.perfil.copy(
        nombre = usuario.nombre,
        // NUEVO (Rol): bajo el nombre se muestra el rol (viene del JOIN) y el correo
        profesion = "${usuario.rolNombre} · ${usuario.correo}",
        ciudad = usuario.ciudad
    )

    PerfilContent(
        uiState = uiState.copy(perfil = perfil),
        onAlternarSeguir = viewModel::onAlternarSeguir,
        onVolver = onVolver
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilContent(
    uiState: PerfilUiState,
    onAlternarSeguir: () -> Unit,
    onVolver: () -> Unit = {}
) {
    val perfil = uiState.perfil

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.titulo_perfil)) },

                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            painter = painterResource(R.drawable.ic_volver),
                            contentDescription = stringResource(R.string.cd_volver)
                        )
                    }
                }
            )
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAlternarSeguir,
                icon = {
                    Icon(
                        painter = painterResource(if (uiState.loSigo) R.drawable.ic_check else R.drawable.ic_seguir),
                        contentDescription = null
                    )
                },
                text = {
                    Text(stringResource(if (uiState.loSigo) R.string.btn_siguiendo else R.string.btn_seguir))
                },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EncabezadoPerfil()

            Spacer(Modifier.height(Dimens.espacioChico))
            Text(text = perfil.nombre, style = MaterialTheme.typography.titleLarge)
            Text(
                text = perfil.profesion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Dimens.espacioMini)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ubicacion),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = perfil.ciudad,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Dimens.espacioMini)
                )
            }

            Column(
                modifier = Modifier.padding(Dimens.espacioPantalla),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                    TarjetaEstadistica(perfil.publicaciones.toString(), "Posts", Modifier.weight(1f))
                    TarjetaEstadistica(perfil.seguidores.toString(), "Seguidores", Modifier.weight(1f))
                    TarjetaEstadistica(perfil.siguiendo.toString(), "Siguiendo", Modifier.weight(1f))
                }

                TarjetaSeccion(titulo = stringResource(R.string.seccion_sobre_mi)) {
                    Text(text = perfil.descripcion, style = MaterialTheme.typography.bodyMedium)
                }

                TarjetaSeccion(titulo = stringResource(R.string.seccion_intereses)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
                    ) {
                        perfil.intereses.forEach { interes ->
                            EtiquetaInteres(texto = interes)
                        }
                    }
                }

                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun EncabezadoPerfil() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.altoPortada + Dimens.tamanoAvatar / 2)
    ) {
        Image(
            painter = painterResource(R.drawable.portada_perfil),
            contentDescription = stringResource(R.string.cd_portada),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.altoPortada)
                .align(Alignment.TopCenter)
        )
        AvatarCircular(
            imagen = R.drawable.avatar_usuario,
            descripcion = stringResource(R.string.cd_avatar),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private val perfilPreview = Perfil(
    nombre = "Camila Rojas",
    profesion = "Desarrolladora móvil",
    ciudad = "Santiago, Chile",
    descripcion = "Texto de ejemplo para la vista previa.",
    seguidores = 128,
    siguiendo = 96,
    publicaciones = 24,
    intereses = listOf("Kotlin", "Compose", "Diseño UI")
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PerfilPreview() {
    MiRegistroTheme {
        PerfilContent(uiState = PerfilUiState(perfil = perfilPreview), onAlternarSeguir = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Modo oscuro")
@Composable
private fun PerfilOscuroPreview() {
    MiRegistroTheme(darkTheme = true) {
        PerfilContent(uiState = PerfilUiState(perfil = perfilPreview, loSigo = true), onAlternarSeguir = {})
    }
}
