package cl.duocuc.miregistro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.EstadoProtocolo
import cl.duocuc.miregistro.model.Medicion
import cl.duocuc.miregistro.model.Protocolo
import cl.duocuc.miregistro.model.TipoEquipo
import cl.duocuc.miregistro.model.aTexto
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.BotonSecundario
import cl.duocuc.miregistro.ui.components.EtiquetaEstado
import cl.duocuc.miregistro.ui.components.LineaDato
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme

// =====================================================================
//  NUEVO (Detalle) — DetalleProtocoloScreen.kt
//  ---------------------------------------------------------------------
//  Comprobante del protocolo: se le muestra al cliente en la pantalla del celular
//  para que tenga un respaldo en el momento (mientras el protocolo está "Registrado
//  en terreno"). De arriba hacia abajo: datos del comprobante, equipo e instalación,
//  tabla de mediciones, resultado y botones.
//  Reglas:
//   - Solo el Supervisor ve "Formalizar", y solo si el protocolo está EN_TERRENO.
//   - Un protocolo FORMALIZADO es de solo lectura: la pantalla no ofrece ninguna edición.
//  Recibe datos y lambdas: NO conoce el ViewModel.
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleProtocoloScreen(
    protocolo: Protocolo,
    esSupervisor: Boolean,
    onVolver: () -> Unit,
    onFormalizar: () -> Unit,
    onVerHistorial: () -> Unit
) {
    // NUEVO (Formalizar): el diálogo se muestra mientras esta variable sea true
    var mostrarDialogo by remember { mutableStateOf(false) }
    val puedeFormalizar = esSupervisor && protocolo.estado == EstadoProtocolo.EN_TERRENO

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text(stringResource(R.string.dialogo_formalizar_titulo)) },
            text = { Text(stringResource(R.string.dialogo_formalizar_mensaje, protocolo.equipoCodigo)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    onFormalizar()
                }) { Text(stringResource(R.string.btn_formalizar)) }
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
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_detalle)) },
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
            TarjetaSeccion(titulo = stringResource(R.string.detalle_numero, protocolo.id)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dato_estado),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    EtiquetaEstado(estado = protocolo.estado)
                }
                LineaDato(stringResource(R.string.dato_fecha), protocolo.fechaTexto)
                LineaDato(stringResource(R.string.dato_tecnico), protocolo.tecnicoNombre)
            }

            TarjetaSeccion(titulo = stringResource(R.string.seccion_equipo)) {
                LineaDato(stringResource(R.string.dato_equipo), protocolo.equipoCodigo)
                LineaDato(stringResource(R.string.dato_tipo), protocolo.equipoTipo.etiqueta)
                LineaDato(stringResource(R.string.dato_cliente), protocolo.cliente)
                LineaDato(stringResource(R.string.dato_planta), protocolo.planta)
            }

            TarjetaSeccion(titulo = stringResource(R.string.seccion_mediciones)) {
                EncabezadoTabla()
                protocolo.mediciones.forEach { medicion ->
                    HorizontalDivider()
                    FilaMedicion(medicion)
                }
            }

            TarjetaSeccion(titulo = stringResource(R.string.dato_resultado)) {
                val aprobado = protocolo.resultado == Protocolo.RESULTADO_APROBADO
                Text(
                    text = stringResource(
                        if (aprobado) R.string.resultado_aprobado else R.string.resultado_rechazado
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (aprobado) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Text(
                    text = protocolo.observaciones.ifBlank { stringResource(R.string.sin_observaciones) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // NUEVO (Formalizar): solo el Supervisor, y solo mientras esté "Registrado en terreno"
            if (puedeFormalizar) {
                BotonPrincipal(
                    texto = stringResource(R.string.btn_formalizar),
                    onClick = { mostrarDialogo = true },
                    icono = R.drawable.ic_check,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // NUEVO (Solo lectura): un protocolo formalizado ya no se puede cambiar
            if (protocolo.estado == EstadoProtocolo.FORMALIZADO) {
                Text(
                    text = stringResource(R.string.aviso_solo_lectura),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            BotonSecundario(
                texto = stringResource(R.string.btn_ver_historial),
                onClick = onVerHistorial,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// NUEVO (Tabla): las 3 columnas usan weight para que todas las filas queden alineadas
@Composable
private fun EncabezadoTabla() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.col_parametro),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1.4f)
        )
        Text(
            text = stringResource(R.string.col_valor),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.col_rango),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1.2f)
        )
    }
}

// NUEVO (Rango): una fila de la tabla. Si la medición está fuera de rango, todo el valor
// se destaca con el color error (rojo) y en negrita, y aparece la etiqueta "Fuera de rango".
@Composable
private fun FilaMedicion(medicion: Medicion) {
    val color = if (medicion.fueraDeRango) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1.4f)) {
            Text(text = medicion.parametro, style = MaterialTheme.typography.bodyMedium, color = color)
            if (medicion.fueraDeRango) {
                Text(
                    text = stringResource(R.string.medicion_etiqueta_fuera),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Text(
            text = "${medicion.valor.aTexto()} ${medicion.unidad}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (medicion.fueraDeRango) FontWeight.Bold else FontWeight.Normal,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${medicion.minimo.aTexto()} – ${medicion.maximo.aTexto()} ${medicion.unidad}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.2f)
        )
    }
}

private val protocoloPreview = Protocolo(
    id = 7, equipoId = 1, usuarioId = 1, fechaHora = 1_760_000_000_000,
    resultado = Protocolo.RESULTADO_RECHAZADO, estado = EstadoProtocolo.EN_TERRENO,
    observaciones = "Aislamiento bajo el mínimo: revisar humedad.",
    equipoCodigo = "TR-001", equipoTipo = TipoEquipo.TRANSFORMADOR,
    planta = "Planta Concentradora Norte", cliente = "Minera Los Andes",
    tecnicoNombre = "Mateo Rivas", medicionesFueraDeRango = 1,
    mediciones = listOf(
        Medicion("Resistencia de aislamiento", 950.0, "MΩ", 1000.0, 5000.0),
        Medicion("Relación de transformación", 20.01, "V/V", 19.8, 20.2)
    )
)

@Preview(showBackground = true, showSystemUi = true, name = "Supervisor, en terreno")
@Composable
private fun DetalleEnTerrenoPreview() {
    ProtoFieldTheme {
        DetalleProtocoloScreen(
            protocolo = protocoloPreview, esSupervisor = true,
            onVolver = {}, onFormalizar = {}, onVerHistorial = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Formalizado, modo oscuro")
@Composable
private fun DetalleFormalizadoPreview() {
    ProtoFieldTheme(darkTheme = true) {
        DetalleProtocoloScreen(
            protocolo = protocoloPreview.copy(estado = EstadoProtocolo.FORMALIZADO),
            esSupervisor = true,
            onVolver = {}, onFormalizar = {}, onVerHistorial = {}
        )
    }
}
