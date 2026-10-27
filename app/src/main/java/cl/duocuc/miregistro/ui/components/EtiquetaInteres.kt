package cl.duocuc.miregistro.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.miregistro.model.EstadoProtocolo
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme


@Composable
fun EtiquetaInteres(
    texto: String,
    modifier: Modifier = Modifier,

    colorFondo: Color? = null,
    colorTexto: Color? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colorFondo ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        contentColor = colorTexto ?: MaterialTheme.colorScheme.secondary
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Dimens.espacioMedio, vertical = Dimens.espacioChico)
        )
    }
}


@Composable
fun EtiquetaEstado(
    estado: EstadoProtocolo,
    modifier: Modifier = Modifier
) {
    val enTerreno = estado == EstadoProtocolo.EN_TERRENO
    EtiquetaInteres(
        texto = estado.etiqueta,
        modifier = modifier,
        colorFondo = if (enTerreno) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        colorTexto = if (enTerreno) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
    )
}

@Preview(showBackground = true)
@Composable
private fun EtiquetaInteresPreview() {
    ProtoFieldTheme  {
        EtiquetaInteres(texto = "Kotlin", modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun EtiquetaEstadoPreview() {
    ProtoFieldTheme {
        Column(Modifier.padding(16.dp)) {
            EtiquetaEstado(estado = EstadoProtocolo.EN_TERRENO)
            EtiquetaEstado(estado = EstadoProtocolo.FORMALIZADO, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
