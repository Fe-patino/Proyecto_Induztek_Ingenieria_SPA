package cl.duocuc.miregistro.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.data.ConvertidorImagen
import cl.duocuc.miregistro.model.ResumenProtocolos
import cl.duocuc.miregistro.model.Rol
import cl.duocuc.miregistro.model.Usuario
import cl.duocuc.miregistro.permisos.Permisos
import cl.duocuc.miregistro.ui.components.AvatarCircular
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.TarjetaEstadistica
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme
import kotlinx.coroutines.launch

// =====================================================================
//  NUEVO (Perfil) — PerfilScreen.kt
//  ---------------------------------------------------------------------
//  Perfil del usuario: avatar (cámara o galería), nombre, correo, rol,
//  estadísticas de sus protocolos (solo el Técnico) y cerrar sesión.
//  Se divide en dos:
//   - PerfilScreen  : maneja la cámara, la galería y los PERMISOS (los 4 pasos
//                     de Permisos.kt: declarar, verificar, pedir, reaccionar)
//   - PerfilContent : solo dibuja; recibe datos y lambdas, por eso tiene @Preview
//  Ninguna de las dos conoce al ViewModel: la foto sale hacia afuera con
//  onFotoTomada(Bitmap) / onImagenElegida(Uri) y AppNavegacion la guarda.
// =====================================================================

@Composable
fun PerfilScreen(
    usuario: Usuario,
    resumen: ResumenProtocolos,
    onFotoTomada: (Bitmap) -> Unit,
    onImagenElegida: (Uri) -> Unit,
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit
) {
    // NUEVO (Permisos): Context = acceso a los servicios de Android (permisos, Ajustes...)
    val context = LocalContext.current

    // NUEVO (Permisos): Snackbar para avisar cuando el usuario rechaza un permiso.
    // stringResource solo funciona en un @Composable: los textos se preparan aquí.
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textoCamaraDenegada = stringResource(R.string.permiso_camara_denegado)
    val textoGaleriaDenegada = stringResource(R.string.permiso_galeria_denegado)
    val textoAjustes = stringResource(R.string.btn_ajustes)

    // NUEVO (Permisos): PASO 4 (rechazo) -> avisar y ofrecer abrir Ajustes.
    // Si el usuario rechazó 2 veces, Android ya no muestra el diálogo: solo queda Ajustes.
    fun avisarPermisoDenegado(mensaje: String) {
        scope.launch {
            val resultado = snackbarHostState.showSnackbar(
                message = mensaje,
                actionLabel = textoAjustes,
                duration = SnackbarDuration.Long
            )
            if (resultado == SnackbarResult.ActionPerformed) Permisos.abrirAjustes(context)
        }
    }

    // ------------------------------- CÁMARA -------------------------------

    // NUEVO (Cámara): "launcher" = lanzador de OTRA app que nos devuelve un resultado.
    // TakePicturePreview abre la app de cámara y entrega un Bitmap (o null si canceló).
    // Como la app DECLARA el permiso CAMERA, Android exige que esté CONCEDIDO
    // antes de abrir la cámara (si no, lanza SecurityException).
    val lanzadorCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { foto: Bitmap? ->
        if (foto != null) onFotoTomada(foto)
    }

    // NUEVO (Cámara): se llama SOLO con el permiso ya concedido.
    // try/catch: si el equipo no tiene app de cámara, la app no se cae.
    fun abrirCamara() {
        try {
            lanzadorCamara.launch(null)
        } catch (e: ActivityNotFoundException) {
        }
    }

    // NUEVO (Permisos): PASO 3 -> PEDIR. RequestPermission muestra el diálogo del sistema
    // "¿Permitir que ProtoField tome fotos y grabe videos?" y entrega true o false.
    val pedirPermisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido: Boolean ->
        // NUEVO (Permisos): PASO 4 -> REACCIONAR a la respuesta del usuario
        if (concedido) abrirCamara() else avisarPermisoDenegado(textoCamaraDenegada)
    }

    // ------------------------------- GALERÍA ------------------------------

    // NUEVO (Galería): StartActivityForResult lanza un Intent cualquiera y devuelve
    // el resultado. ACTION_PICK + tabla de imágenes = "abre la galería para elegir una foto".
    // RESULT_OK = el usuario eligió algo;  data?.data = Uri de la imagen elegida.
    val lanzadorGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val uri = resultado.data?.data
        if (resultado.resultCode == Activity.RESULT_OK && uri != null) onImagenElegida(uri)
    }

    // NUEVO (Galería): se llama SOLO con el permiso de imágenes ya concedido.
    fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            lanzadorGaleria.launch(intent)
        } catch (e: ActivityNotFoundException) {
        }
    }

    // NUEVO (Permisos): PASO 3 -> PEDIR el permiso de imágenes
    // (READ_MEDIA_IMAGES o READ_EXTERNAL_STORAGE según la versión: ver Permisos.IMAGENES)
    val pedirPermisoImagenes = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido: Boolean ->
        // NUEVO (Permisos): PASO 4 -> REACCIONAR
        if (concedido) abrirGaleria() else avisarPermisoDenegado(textoGaleriaDenegada)
    }

    // NUEVO (Avatar): bytes de la BD -> Bitmap -> ImageBitmap (el tipo que usa Compose).
    // remember(usuario.avatar): se convierte solo cuando la foto CAMBIA, no en cada dibujo.
    val foto = remember(usuario.avatar) {
        usuario.avatar?.let { bytes -> ConvertidorImagen.aBitmap(bytes)?.asImageBitmap() }
    }

    PerfilContent(
        usuario = usuario,
        foto = foto,
        resumen = resumen,
        onTomarFoto = {
            // NUEVO (Permisos): PASO 2 -> VERIFICAR. ¿Ya tengo permiso? Uso la cámara.
            // ¿No? Lo pido (PASO 3) y la respuesta llega a pedirPermisoCamara (PASO 4).
            if (Permisos.tiene(context, Permisos.CAMARA)) abrirCamara()
            else pedirPermisoCamara.launch(Permisos.CAMARA)
        },
        onAbrirGaleria = {
            // NUEVO (Permisos): PASO 2 -> VERIFICAR el permiso de imágenes
            if (Permisos.tiene(context, Permisos.IMAGENES)) abrirGaleria()
            else pedirPermisoImagenes.launch(Permisos.IMAGENES)
        },
        onCerrarSesion = onCerrarSesion,
        onVolver = onVolver,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilContent(
    usuario: Usuario,
    foto: ImageBitmap?,
    resumen: ResumenProtocolos,
    onTomarFoto: () -> Unit,
    onAbrirGaleria: () -> Unit,
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    // NUEVO (Sesión): el diálogo se muestra mientras esta variable sea true
    var mostrarDialogo by remember { mutableStateOf(false) }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text(stringResource(R.string.dialogo_titulo)) },
            text = { Text(stringResource(R.string.dialogo_mensaje)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    onCerrarSesion()
                }) { Text(stringResource(R.string.menu_cerrar_sesion)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text(stringResource(R.string.dialogo_cancelar))
                }
            }
        )
    }

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EncabezadoPerfil(foto = foto)

            Spacer(Modifier.height(Dimens.espacioChico))
            Text(text = usuario.nombre, style = MaterialTheme.typography.titleLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Dimens.espacioMini)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_correo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = usuario.correo,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = Dimens.espacioMini)
                )
            }
            Text(
                text = usuario.rolNombre,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Dimens.espacioMini)
            )

            Column(
                modifier = Modifier.padding(Dimens.espacioPantalla),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
            ) {
                // NUEVO (Rol): las estadísticas son del Técnico (los protocolos que él realizó)
                if (usuario.rolId == Rol.ID_TECNICO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                        TarjetaEstadistica(
                            resumen.total.toString(),
                            stringResource(R.string.perfil_realizados),
                            Modifier.weight(1f)
                        )
                        TarjetaEstadistica(
                            resumen.enTerreno.toString(),
                            stringResource(R.string.estadistica_en_terreno),
                            Modifier.weight(1f)
                        )
                        TarjetaEstadistica(
                            resumen.formalizados.toString(),
                            stringResource(R.string.estadistica_formalizados),
                            Modifier.weight(1f)
                        )
                    }
                }

                TarjetaSeccion(titulo = stringResource(R.string.texto_cambiar_foto)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                        BotonPrincipal(
                            texto = stringResource(R.string.btn_camara),
                            onClick = onTomarFoto,
                            icono = R.drawable.ic_camara,
                            modifier = Modifier.weight(1f)
                        )
                        BotonSecundario(
                            texto = stringResource(R.string.btn_galeria),
                            onClick = onAbrirGaleria,
                            icono = R.drawable.ic_galeria,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                BotonSecundario(
                    texto = stringResource(R.string.menu_cerrar_sesion),
                    onClick = { mostrarDialogo = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// NUEVO (Perfil): encabezado con el color primary del tema (ya no hay imagen de portada).
// El avatar queda a medias sobre el borde inferior del encabezado.
@Composable
private fun EncabezadoPerfil(foto: ImageBitmap?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.altoPortada + Dimens.tamanoAvatar / 2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.altoPortada)
                .background(MaterialTheme.colorScheme.primary)
                .align(Alignment.TopCenter)
        )
        AvatarCircular(
            imagen = R.drawable.avatar_usuario,
            descripcion = stringResource(R.string.cd_avatar),
            foto = foto,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private val tecnicoPreview = Usuario(
    nombre = "Mateo Rivas",
    correo = "mateo.rivas@induztek.cl",
    rolId = Rol.ID_TECNICO,
    rolNombre = "Técnico"
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PerfilPreview() {
    ProtoFieldTheme {
        PerfilContent(
            usuario = tecnicoPreview,
            foto = null,
            resumen = ResumenProtocolos(total = 8, enTerreno = 2, formalizados = 6),
            onTomarFoto = {}, onAbrirGaleria = {}, onCerrarSesion = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Supervisor, modo oscuro")
@Composable
private fun PerfilSupervisorOscuroPreview() {
    ProtoFieldTheme(darkTheme = true) {
        PerfilContent(
            usuario = Usuario(
                nombre = "Carlos Fuentes",
                correo = "carlos.fuentes@induztek.cl",
                rolId = Rol.ID_SUPERVISOR,
                rolNombre = "Supervisor"
            ),
            foto = null,
            resumen = ResumenProtocolos(),
            onTomarFoto = {}, onAbrirGaleria = {}, onCerrarSesion = {}
        )
    }
}
