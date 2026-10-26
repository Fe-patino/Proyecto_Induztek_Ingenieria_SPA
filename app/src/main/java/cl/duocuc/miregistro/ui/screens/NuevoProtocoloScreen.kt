package cl.duocuc.miregistro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.Equipo
import cl.duocuc.miregistro.model.Instalacion
import cl.duocuc.miregistro.model.NuevoProtocoloUiState
import cl.duocuc.miregistro.model.Protocolo
import cl.duocuc.miregistro.model.TipoEquipo
import cl.duocuc.miregistro.model.aTexto
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.GrupoRadio
import cl.duocuc.miregistro.ui.components.LineaDato
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.styles.EstilosCampo
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme
import cl.duocuc.miregistro.viewmodel.NuevoProtocoloViewModel

// =====================================================================
//  NUEVO (Nuevo protocolo) — NuevoProtocoloScreen.kt
//  ---------------------------------------------------------------------
//  Formulario de 3 pasos en UNA sola pantalla (no son 3 rutas): según uiState.paso
//  se dibuja un contenido distinto, y arriba siempre aparece "Paso X de 3".
//    Paso 1: elegir el equipo (se muestran su tipo y su planta)
//    Paso 2: un campo numérico por cada parámetro del tipo de equipo
//    Paso 3: resultado Aprobado/Rechazado, observaciones y resumen
//  Se divide en dos, igual que LoginScreen:
//   - NuevoProtocoloScreen  : conecta con el NuevoProtocoloViewModel
//   - NuevoProtocoloContent : solo dibuja; por eso tiene @Preview
// =====================================================================

@Composable
fun NuevoProtocoloScreen(
    equipos: List<Equipo>,
    usuarioId: Long,
    onVolver: () -> Unit,
    onGuardar: (Protocolo) -> Unit,
    viewModel: NuevoProtocoloViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NuevoProtocoloContent(
        uiState = uiState,
        equipos = equipos,
        onEquipoSeleccionado = viewModel::onEquipoSeleccionado,
        onValorCambia = viewModel::onValorCambia,
        onResultadoCambia = viewModel::onResultadoCambia,
        onObservacionesCambia = viewModel::onObservacionesCambia,
        onAnterior = viewModel::onAnterior,
        onSiguiente = viewModel::onSiguiente,
        // NUEVO (Guardar): el ViewModel valida y arma el Protocolo; si es válido, se entrega
        onGuardar = { viewModel.onGuardar(usuarioId)?.let(onGuardar) },
        onVolver = onVolver
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevoProtocoloContent(
    uiState: NuevoProtocoloUiState,
    equipos: List<Equipo>,
    onEquipoSeleccionado: (Equipo) -> Unit,
    onValorCambia: (Int, String) -> Unit,
    onResultadoCambia: (String) -> Unit,
    onObservacionesCambia: (String) -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onGuardar: () -> Unit,
    onVolver: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_nuevo_protocolo)) },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            // NUEVO (Pasos): indicador "Paso X de 3"
            Text(
                text = stringResource(
                    R.string.paso_indicador, uiState.paso, NuevoProtocoloUiState.TOTAL_PASOS
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // NUEVO (Pasos): un solo composable cambia de contenido según el paso
            when (uiState.paso) {
                1 -> PasoEquipo(uiState, equipos, onEquipoSeleccionado)
                2 -> PasoMediciones(uiState, onValorCambia)
                else -> PasoCierre(uiState, onResultadoCambia, onObservacionesCambia)
            }

            // Botones de 56 dp (Dimens.alturaBoton): se usan con guantes en terreno
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                if (uiState.paso > 1) {
                    BotonSecundario(
                        texto = stringResource(R.string.btn_anterior),
                        onClick = onAnterior,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (uiState.paso < NuevoProtocoloUiState.TOTAL_PASOS) {
                    BotonPrincipal(
                        texto = stringResource(R.string.btn_siguiente),
                        onClick = onSiguiente,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    BotonPrincipal(
                        texto = stringResource(R.string.btn_guardar_protocolo),
                        onClick = onGuardar,
                        icono = R.drawable.ic_check,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ----------------------------- PASO 1 -----------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasoEquipo(
    uiState: NuevoProtocoloUiState,
    equipos: List<Equipo>,
    onEquipoSeleccionado: (Equipo) -> Unit
) {
    TarjetaSeccion(titulo = stringResource(R.string.paso_1_titulo)) {
        var expandido by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expandido,
            onExpandedChange = { expandido = it }
        ) {
            OutlinedTextField(
                value = uiState.equipo?.let { "${it.codigo} · ${it.tipo.etiqueta}" } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.campo_equipo)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                isError = uiState.errorEquipo != null,
                supportingText = { uiState.errorEquipo?.let { Text(stringResource(it)) } },
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
                equipos.forEach { equipo ->
                    DropdownMenuItem(
                        text = { Text("${equipo.codigo} · ${equipo.tipo.etiqueta}") },
                        onClick = {
                            onEquipoSeleccionado(equipo)
                            expandido = false
                        }
                    )
                }
            }
        }
    }

    // NUEVO (Instalación): al elegir un equipo se muestran su tipo y la planta del cliente
    uiState.equipo?.let { equipo ->
        TarjetaSeccion(titulo = stringResource(R.string.seccion_instalacion)) {
            LineaDato(stringResource(R.string.dato_tipo), equipo.tipo.etiqueta)
            LineaDato(stringResource(R.string.dato_marca), equipo.marca)
            LineaDato(stringResource(R.string.dato_cliente), equipo.instalacion.cliente)
            LineaDato(stringResource(R.string.dato_planta), equipo.instalacion.planta)
            LineaDato(stringResource(R.string.dato_ciudad), equipo.instalacion.ciudad)
        }
    }
}

// ----------------------------- PASO 2 -----------------------------

@Composable
private fun PasoMediciones(
    uiState: NuevoProtocoloUiState,
    onValorCambia: (Int, String) -> Unit
) {
    val equipo = uiState.equipo ?: return

    TarjetaSeccion(titulo = stringResource(R.string.paso_2_titulo)) {
        // NUEVO (Mediciones): el formulario se GENERA solo, un campo por cada parámetro del tipo
        equipo.tipo.parametros.forEachIndexed { indice, parametro ->
            val error = uiState.erroresMediciones.getOrNull(indice)
            val fueraDeRango = uiState.fueraDeRango(indice)

            OutlinedTextField(
                value = uiState.valores.getOrElse(indice) { "" },
                onValueChange = { onValorCambia(indice, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("${parametro.nombre} (${parametro.unidad})") },
                // NUEVO (Rango): rojo si falta el valor o si está fuera de rango
                isError = error != null || fueraDeRango,
                supportingText = {
                    Text(
                        when {
                            error != null -> stringResource(error)
                            fueraDeRango -> stringResource(
                                R.string.medicion_fuera_de_rango,
                                parametro.minimo.aTexto(), parametro.maximo.aTexto(), parametro.unidad
                            )
                            else -> stringResource(
                                R.string.medicion_rango,
                                parametro.minimo.aTexto(), parametro.maximo.aTexto(), parametro.unidad
                            )
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                shape = EstilosCampo.forma,
                colors = EstilosCampo.colores()
            )
        }
    }
}

// ----------------------------- PASO 3 -----------------------------

@Composable
private fun PasoCierre(
    uiState: NuevoProtocoloUiState,
    onResultadoCambia: (String) -> Unit,
    onObservacionesCambia: (String) -> Unit
) {
    val equipo = uiState.equipo ?: return
    val textoAprobado = stringResource(R.string.resultado_aprobado)
    val textoRechazado = stringResource(R.string.resultado_rechazado)
    val mediciones = uiState.mediciones()
    val cantidadFuera = mediciones.count { it.fueraDeRango }

    TarjetaSeccion(titulo = stringResource(R.string.paso_3_titulo)) {
        // textoDe no es @Composable: los textos se leen de strings.xml antes de usarlos
        GrupoRadio(
            opciones = listOf(Protocolo.RESULTADO_APROBADO, Protocolo.RESULTADO_RECHAZADO),
            seleccionada = uiState.resultado,
            onSeleccionar = onResultadoCambia,
            textoDe = { if (it == Protocolo.RESULTADO_APROBADO) textoAprobado else textoRechazado }
        )
        // NUEVO (Rango): aviso, no bloqueo: el técnico decide el resultado
        if (cantidadFuera > 0 && uiState.resultado == Protocolo.RESULTADO_APROBADO) {
            Text(
                text = stringResource(R.string.aviso_aprobado_fuera_de_rango),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        OutlinedTextField(
            value = uiState.observaciones,
            onValueChange = onObservacionesCambia,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.campo_observaciones)) },
            minLines = 3,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = EstilosCampo.forma,
            colors = EstilosCampo.colores()
        )
    }

    // NUEVO (Resumen): lo que se va a guardar. La fecha se toma automáticamente al guardar.
    TarjetaSeccion(titulo = stringResource(R.string.seccion_resumen)) {
        LineaDato(stringResource(R.string.dato_equipo), "${equipo.codigo} · ${equipo.tipo.etiqueta}")
        LineaDato(stringResource(R.string.dato_planta), equipo.instalacion.planta)
        Text(
            text = stringResource(R.string.resumen_fecha_automatica),
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider()
        mediciones.forEach { medicion ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = medicion.parametro,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${medicion.valor.aTexto()} ${medicion.unidad}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (medicion.fueraDeRango) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private val equiposPreview = listOf(
    Equipo(
        id = 1, codigo = "TR-001", tipo = TipoEquipo.TRANSFORMADOR, marca = "Siemens",
        instalacion = Instalacion(1, "Minera Los Andes", "Planta Concentradora Norte", "Antofagasta")
    ),
    Equipo(
        id = 2, codigo = "RL-101", tipo = TipoEquipo.RELE, marca = "SEL",
        instalacion = Instalacion(1, "Minera Los Andes", "Planta Concentradora Norte", "Antofagasta")
    )
)

@Preview(showBackground = true, showSystemUi = true, name = "Paso 1")
@Composable
private fun NuevoProtocoloPaso1Preview() {
    ProtoFieldTheme {
        NuevoProtocoloContent(
            uiState = NuevoProtocoloUiState(equipo = equiposPreview[0]),
            equipos = equiposPreview,
            onEquipoSeleccionado = {}, onValorCambia = { _, _ -> }, onResultadoCambia = {},
            onObservacionesCambia = {}, onAnterior = {}, onSiguiente = {}, onGuardar = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Paso 2 con valor fuera de rango")
@Composable
private fun NuevoProtocoloPaso2Preview() {
    ProtoFieldTheme {
        NuevoProtocoloContent(
            uiState = NuevoProtocoloUiState(
                paso = 2,
                equipo = equiposPreview[0],
                valores = listOf("950", "")
            ),
            equipos = equiposPreview,
            onEquipoSeleccionado = {}, onValorCambia = { _, _ -> }, onResultadoCambia = {},
            onObservacionesCambia = {}, onAnterior = {}, onSiguiente = {}, onGuardar = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Paso 3, modo oscuro")
@Composable
private fun NuevoProtocoloPaso3Preview() {
    ProtoFieldTheme(darkTheme = true) {
        NuevoProtocoloContent(
            uiState = NuevoProtocoloUiState(
                paso = 3,
                equipo = equiposPreview[0],
                valores = listOf("950", "20.01")
            ),
            equipos = equiposPreview,
            onEquipoSeleccionado = {}, onValorCambia = { _, _ -> }, onResultadoCambia = {},
            onObservacionesCambia = {}, onAnterior = {}, onSiguiente = {}, onGuardar = {}
        )
    }
}
