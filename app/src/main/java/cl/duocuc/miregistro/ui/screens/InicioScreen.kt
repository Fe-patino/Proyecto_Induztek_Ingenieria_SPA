package cl.duocuc.miregistro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoProtocolo
import cl.duocuc.miregistro.model.Protocolo
import cl.duocuc.miregistro.model.ResumenProtocolos
import cl.duocuc.miregistro.model.Rol
import cl.duocuc.miregistro.model.TipoEquipo
import cl.duocuc.miregistro.model.Usuario
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.CampoTexto
import cl.duocuc.miregistro.ui.components.EtiquetaEstado
import cl.duocuc.miregistro.ui.components.TarjetaEstadistica
import cl.duocuc.miregistro.ui.styles.EstilosCampo
import cl.duocuc.miregistro.ui.styles.EstilosTarjeta
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme

// =====================================================================
//  NUEVO (Inicio) — InicioScreen.kt
//  ---------------------------------------------------------------------
//  Dashboard de ProtoField. De arriba hacia abajo:
//   1) Saludo con el nombre y el rol
//   2) 4 tarjetas estadísticas (total, en terreno, formalizados, fuera de rango)
//   3) Buscador por código de equipo + filtro por estado
//   4) Botón "Nuevo protocolo" (solo el Técnico)
//   5) Lista de protocolos: el Técnico ve solo los suyos, el Supervisor todos
//  Todo va en UNA LazyColumn: el encabezado son "item" y los protocolos "items",
//  así toda la pantalla se desplaza junta. Recibe datos y lambdas: NO conoce el ViewModel.
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    usuario: Usuario,
    resumen: ResumenProtocolos,
    protocolos: List<Protocolo>,
    filtro: String,
    estadoFiltro: EstadoProtocolo?,
    onBuscar: (String) -> Unit,
    onFiltrarEstado: (EstadoProtocolo?) -> Unit,
    onNuevoProtocolo: () -> Unit,
    onVerProtocolo: (Long) -> Unit,
    onVerPerfil: () -> Unit,
    onCerrarSesion: () -> Unit
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

    val esTecnico = usuario.rolId == Rol.ID_TECNICO

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onVerPerfil) {
                        Icon(
                            painter = painterResource(R.drawable.ic_persona),
                            contentDescription = stringResource(R.string.menu_mi_perfil)
                        )
                    }
                    // NUEVO (Sesión): el candado abre el diálogo de confirmación. Solo se
                    // cierra la sesión si el usuario confirma (ver AlertDialog más arriba).
                    IconButton(onClick = { mostrarDialogo = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_candado),
                            contentDescription = stringResource(R.string.menu_cerrar_sesion)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            // 1) Saludo
            item {
                Column {
                    Text(
                        text = stringResource(R.string.saludo, usuario.nombre),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.inicio_rol, usuario.rolNombre),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 2) Tarjetas estadísticas: 2 filas de 2 (en 4 columnas no cabría el texto)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                        TarjetaEstadistica(
                            resumen.total.toString(),
                            stringResource(R.string.estadistica_total),
                            Modifier.weight(1f)
                        )
                        TarjetaEstadistica(
                            resumen.enTerreno.toString(),
                            stringResource(R.string.estadistica_en_terreno),
                            Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                        TarjetaEstadistica(
                            resumen.formalizados.toString(),
                            stringResource(R.string.estadistica_formalizados),
                            Modifier.weight(1f)
                        )
                        TarjetaEstadistica(
                            resumen.fueraDeRango.toString(),
                            stringResource(R.string.estadistica_fuera_de_rango),
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3) Buscador y filtro por estado
            item {
                CampoTexto(
                    valor = filtro,
                    onValorCambia = onBuscar,
                    etiqueta = stringResource(R.string.campo_buscar_codigo),
                    icono = R.drawable.ic_buscar
                )
            }
            item {
                // NUEVO (Filtrar): desplegable. null representa la opción "Todos".
                var expandido by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandido,
                    onExpandedChange = { expandido = it }
                ) {
                    OutlinedTextField(
                        value = textoFiltro(estadoFiltro),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.campo_filtro_estado)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                        shape = EstilosCampo.forma,
                        colors = EstilosCampo.colores(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandido,
                        onDismissRequest = { expandido = false }
                    ) {
                        listOf(null, EstadoProtocolo.EN_TERRENO, EstadoProtocolo.FORMALIZADO)
                            .forEach { estado ->
                                DropdownMenuItem(
                                    text = { Text(textoFiltro(estado)) },
                                    onClick = {
                                        onFiltrarEstado(estado)
                                        expandido = false
                                    }
                                )
                            }
                    }
                }
            }

            // 4) Nuevo protocolo: solo el Técnico registra pruebas en terreno
            if (esTecnico) {
                item {
                    BotonPrincipal(
                        texto = stringResource(R.string.btn_nuevo_protocolo),
                        onClick = onNuevoProtocolo,
                        icono = R.drawable.ic_check,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5) Lista de protocolos
            item {
                Text(
                    text = stringResource(
                        if (esTecnico) R.string.lista_titulo_tecnico else R.string.lista_titulo_supervisor
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (protocolos.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.lista_vacia_protocolos),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            // NUEVO (Lista): key = id -> Compose reconoce cada fila aunque la lista se filtre
            items(protocolos, key = { it.id }) { protocolo ->
                TarjetaProtocolo(
                    protocolo = protocolo,
                    mostrarTecnico = !esTecnico,
                    onClick = { onVerProtocolo(protocolo.id) }
                )
            }
        }
    }
}

// NUEVO (Filtrar): texto de cada opción del desplegable (null = Todos)
@Composable
private fun textoFiltro(estado: EstadoProtocolo?): String = stringResource(
    when (estado) {
        null -> R.string.filtro_todos
        EstadoProtocolo.EN_TERRENO -> R.string.filtro_en_terreno
        EstadoProtocolo.FORMALIZADO -> R.string.filtro_formalizados
    }
)

// NUEVO (Lista): una fila de la lista = una Card tocable con los datos del protocolo
@Composable
private fun TarjetaProtocolo(
    protocolo: Protocolo,
    mostrarTecnico: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = EstilosTarjeta.forma,
        colors = EstilosTarjeta.colores(),
        elevation = EstilosTarjeta.elevacion()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.espacioMedio),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = protocolo.equipoCodigo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                EtiquetaEstado(estado = protocolo.estado)
            }
            Text(text = protocolo.equipoTipo.etiqueta, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${protocolo.cliente} · ${protocolo.planta}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = protocolo.fechaTexto, style = MaterialTheme.typography.bodyMedium)
            // NUEVO (Rol): el Supervisor necesita saber qué técnico hizo la prueba
            if (mostrarTecnico) {
                Text(
                    text = stringResource(R.string.protocolo_tecnico, protocolo.tecnicoNombre),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // NUEVO (Medición): aviso en rojo solo si alguna medición salió del rango
            if (protocolo.medicionesFueraDeRango > 0) {
                Text(
                    text = stringResource(R.string.protocolo_fuera_de_rango, protocolo.medicionesFueraDeRango),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private val tecnicoPreview = Usuario(
    nombre = "Mateo Rivas",
    correo = "mateo.rivas@induztek.cl",
    rolId = Rol.ID_TECNICO,
    rolNombre = "Técnico"
)

private val protocolosPreview = listOf(
    Protocolo(
        id = 1, equipoId = 1, usuarioId = 1, fechaHora = 1_760_000_000_000,
        resultado = Protocolo.RESULTADO_APROBADO, estado = EstadoProtocolo.EN_TERRENO,
        equipoCodigo = "TR-001", equipoTipo = TipoEquipo.TRANSFORMADOR,
        planta = "Planta Concentradora Norte", cliente = "Minera Los Andes",
        tecnicoNombre = "Mateo Rivas"
    ),
    Protocolo(
        id = 2, equipoId = 3, usuarioId = 2, fechaHora = 1_750_000_000_000,
        resultado = Protocolo.RESULTADO_RECHAZADO, estado = EstadoProtocolo.FORMALIZADO,
        equipoCodigo = "CE-201", equipoTipo = TipoEquipo.CELDA,
        planta = "Planta de Celulosa", cliente = "Papelera del Sur",
        tecnicoNombre = "Sofía Araya", medicionesFueraDeRango = 1
    )
)

@Preview(showBackground = true, showSystemUi = true, name = "Técnico")
@Composable
private fun InicioTecnicoPreview() {
    ProtoFieldTheme {
        InicioScreen(
            usuario = tecnicoPreview,
            resumen = ResumenProtocolos(total = 2, enTerreno = 1, formalizados = 1, fueraDeRango = 1),
            protocolos = protocolosPreview,
            filtro = "", estadoFiltro = null,
            onBuscar = {}, onFiltrarEstado = {}, onNuevoProtocolo = {},
            onVerProtocolo = {}, onVerPerfil = {}, onCerrarSesion = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Supervisor, modo oscuro")
@Composable
private fun InicioSupervisorOscuroPreview() {
    ProtoFieldTheme(darkTheme = true) {
        InicioScreen(
            usuario = Usuario(
                nombre = "Carlos Fuentes",
                correo = "carlos.fuentes@induztek.cl",
                rolId = Rol.ID_SUPERVISOR,
                rolNombre = "Supervisor"
            ),
            resumen = ResumenProtocolos(total = 2, enTerreno = 1, formalizados = 1, fueraDeRango = 1),
            protocolos = protocolosPreview,
            filtro = "", estadoFiltro = EstadoProtocolo.EN_TERRENO,
            onBuscar = {}, onFiltrarEstado = {}, onNuevoProtocolo = {},
            onVerProtocolo = {}, onVerPerfil = {}, onCerrarSesion = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Sin resultados")
@Composable
private fun InicioVacioPreview() {
    ProtoFieldTheme {
        InicioScreen(
            usuario = tecnicoPreview,
            resumen = ResumenProtocolos(),
            protocolos = emptyList(),
            filtro = "XX", estadoFiltro = null,
            onBuscar = {}, onFiltrarEstado = {}, onNuevoProtocolo = {},
            onVerProtocolo = {}, onVerPerfil = {}, onCerrarSesion = {}
        )
    }
}
