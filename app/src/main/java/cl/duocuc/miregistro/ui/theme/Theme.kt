package cl.duocuc.miregistro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// xxx = color de fondo / onXxx = color del texto que va encima

private val EsquemaClaro = lightColorScheme(
    primary = Primario,                 // Botones, barra superior, FAB
    onPrimary = Blanco,
    primaryContainer = PrimarioClaro,   // Tarjetas de métricas, chip seleccionado
    onPrimaryContainer = PrimarioOscuro,
    secondary = Secundario,             // Elementos secundarios
    onSecondary = Blanco,
    tertiary = Acento,                  // Alertas, pendiente de sincronizar
    onTertiary = TextoOscuro,           // Texto oscuro: el blanco no se lee sobre amarillo
    error = Error,                      // Errores y mediciones fuera de rango
    onError = Blanco,
    background = FondoClaro,            // Fondo de las pantallas
    onBackground = TextoOscuro,
    surface = SuperficieClara,          // Tarjetas, campos, diálogos
    onSurface = TextoOscuro
)

private val EsquemaOscuro = darkColorScheme(
    primary = PrimarioModoOscuro,
    onPrimary = PrimarioOscuro,
    primaryContainer = PrimarioOscuro,
    onPrimaryContainer = PrimarioClaro,
    secondary = TextoClaro,
    onSecondary = FondoOscuro,
    tertiary = Acento,
    onTertiary = TextoOscuro,
    error = Error,
    onError = Blanco,
    background = FondoOscuro,
    onBackground = TextoClaro,
    surface = SuperficieOscura,
    onSurface = TextoClaro
)

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),   // Chips, campos
    medium = RoundedCornerShape(12.dp), // Tarjetas, botones
    large = RoundedCornerShape(16.dp)   // Diálogos, hojas inferiores
)

@Composable
fun ProtoFieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Tipografia,
        shapes = Formas,
        content = content
    )
}