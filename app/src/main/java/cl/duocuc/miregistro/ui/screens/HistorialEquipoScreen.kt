package cl.duocuc.miregistro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.Equipo
import cl.duocuc.miregistro.model.EstadoProtocolo
import cl.duocuc.miregistro.model.Instalacion
import cl.duocuc.miregistro.model.Medicion
import cl.duocuc.miregistro.model.Protocolo
import cl.duocuc.miregistro.model.TipoEquipo
import cl.duocuc.miregistro.model.aTexto
import cl.duocuc.miregistro.ui.components.EtiquetaEstado
import cl.duocuc.miregistro.ui.components.LineaDato
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme
import kotlin.math.roundToLong

// =====================================================================
//  NUEVO (Historial) — HistorialEquipoScreen.kt
//  ---------------------------------------------------------------------
//  Todas las pruebas de UN equipo, de la más reciente a la más antigua.
//  Cada medición se compara con la MISMA medición (mismo parámetro) de la prueba
//  anterior, que en la lista es la fila de más abajo:
//
//      protocolos[0]  (hoy)         ──compara con──▶  protocolos[1]
//      protocolos[1]                ──compara con──▶  protocolos[2]
//      protocolos[2]  (la primera)  ──no tiene anterior──
//
//  Si una medición empeoró hasta quedar fuera de rango, se destaca con el color error.
//  Recibe datos y lambdas: NO conoce el ViewModel.
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialEquipoScreen(
    equipo: Equipo,
    protocolos: List<Protocolo>,
    onVolver: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_historial)) },
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
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            item {
                TarjetaSeccion(titulo = equipo.codigo) {
                    LineaDato(stringResource(R.string.dato_tipo), equipo.tipo.etiqueta)
                    LineaDato(stringResource(R.string.dato_marca), equipo.marca)
                    LineaDato(stringResource(R.string.dato_cliente), equipo.instalacion.cliente)
                    LineaDato(stringResource(R.string.dato_planta), equipo.instalacion.planta)
                    LineaDato(stringResource(R.string.dato_ciudad), equipo.instalacion.ciudad)
                }
            }

            item {
                Text(
                    text = stringResource(R.string.historial_pruebas),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (protocolos.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.historial_vacio),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // NUEVO (Historial): itemsIndexed entrega también la posición, necesaria para
            // encontrar la prueba anterior (la siguiente fila de la lista)
            itemsIndexed(protocolos, key = { _, protocolo -> protocolo.id }) { indice, protocolo ->
                val anterior = protocolos.getOrNull(indice + 1)
                TarjetaPrueba(protocolo = protocolo, anterior = anterior)
            }
        }
    }
}

// NUEVO (Historial): una prueba = una tarjeta con sus datos y sus mediciones comparadas
@Composable
private fun TarjetaPrueba(
    protocolo: Protocolo,
    anterior: Protocolo?
) {
    TarjetaSeccion(titulo = protocolo.fechaTexto) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.protocolo_tecnico, protocolo.tecnicoNombre),
                style = MaterialTheme.typography.bodyMedium
            )
            EtiquetaEstado(estado = protocolo.estado)
        }

        val aprobado = protocolo.resultado == Protocolo.RESULTADO_APROBADO
        Text(
            text = stringResource(
                if (aprobado) R.string.resultado_aprobado else R.string.resultado_rechazado
            ),
            style = MaterialTheme.typography.titleMedium,
            color = if (aprobado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        protocolo.mediciones.forEach { medicion ->
            HorizontalDivider()
            // NUEVO (Historial): se busca en la prueba anterior el MISMO parámetro
            val medicionAnterior = anterior?.mediciones?.firstOrNull { it.parametro == medicion.parametro }
            FilaComparacion(
                medicion = medicion,
                anterior = medicionAnterior,
                hayPruebaAnterior = anterior != null
            )
        }
    }
}

@Composable
private fun FilaComparacion(
    medicion: Medicion,
    anterior: Medicion?,
    hayPruebaAnterior: Boolean
) {
    // NUEVO (Historial): "empeoró hasta quedar fuera de rango" = antes estaba dentro y ahora no
    val salioDelRango = medicion.fueraDeRango && anterior != null && !anterior.fueraDeRango
    val colorValor = if (medicion.fueraDeRango) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
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
                fontWeight = if (medicion.fueraDeRango) FontWeight.Bold else FontWeight.Normal,
                color = colorValor
            )
        }

        // NUEVO (Historial): diferencia = valor de hoy - valor de la prueba anterior.
        // Se redondea a 3 decimales para esconder el error de los decimales binarios
        // (20.01 - 20.02 daría -0.00999999999...).
        val diferencia = anterior?.let { ((medicion.valor - it.valor) * 1000).roundToLong() / 1000.0 }
        val textoDiferencia = when {
            !hayPruebaAnterior || diferencia == null -> stringResource(R.string.diferencia_primera)
            diferencia == 0.0 -> stringResource(R.string.diferencia_igual)
            diferencia > 0 -> stringResource(R.string.diferencia_sube, diferencia.aTexto(), medicion.unidad)
            else -> stringResource(R.string.diferencia_baja, (-diferencia).aTexto(), medicion.unidad)
        }
        Text(
            text = textoDiferencia,
            style = MaterialTheme.typography.bodyMedium,
            // La diferencia se pinta de rojo si la medición actual está fuera de rango
            color = if (medicion.fueraDeRango) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.secondary
        )
        if (salioDelRango) {
            Text(
                text = stringResource(R.string.medicion_salio_de_rango),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val equipoPreview = Equipo(
    id = 1, codigo = "TR-001", tipo = TipoEquipo.TRANSFORMADOR, marca = "Siemens",
    instalacion = Instalacion(1, "Minera Los Andes", "Planta Concentradora Norte", "Antofagasta")
)

private fun pruebaPreview(
    id: Long,
    fechaHora: Long,
    estado: EstadoProtocolo,
    aislamiento: Double,
    relacion: Double
) = Protocolo(
    id = id, equipoId = 1, usuarioId = 1, fechaHora = fechaHora,
    resultado = if (aislamiento < 1000.0) Protocolo.RESULTADO_RECHAZADO else Protocolo.RESULTADO_APROBADO,
    estado = estado, equipoCodigo = "TR-001", equipoTipo = TipoEquipo.TRANSFORMADOR,
    tecnicoNombre = "Mateo Rivas",
    mediciones = listOf(
        Medicion("Resistencia de aislamiento", aislamiento, "MΩ", 1000.0, 5000.0),
        Medicion("Relación de transformación", relacion, "V/V", 19.8, 20.2)
    )
)

// La más reciente primero: el aislamiento baja 3650 -> 3100 -> 950 (sale del rango)
private val pruebasPreview = listOf(
    pruebaPreview(3, 1_760_000_000_000, EstadoProtocolo.EN_TERRENO, 950.0, 19.98),
    pruebaPreview(2, 1_745_000_000_000, EstadoProtocolo.FORMALIZADO, 3100.0, 20.02),
    pruebaPreview(1, 1_730_000_000_000, EstadoProtocolo.FORMALIZADO, 3650.0, 20.01)
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HistorialPreview() {
    ProtoFieldTheme {
        HistorialEquipoScreen(equipo = equipoPreview, protocolos = pruebasPreview, onVolver = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Modo oscuro")
@Composable
private fun HistorialOscuroPreview() {
    ProtoFieldTheme(darkTheme = true) {
        HistorialEquipoScreen(equipo = equipoPreview, protocolos = pruebasPreview, onVolver = {})
    }
}
