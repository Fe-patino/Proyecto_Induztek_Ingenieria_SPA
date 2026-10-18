package cl.duocuc.miregistro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Fuente = FontFamily.SansSerif

val Tipografia = Typography(
    titleLarge = TextStyle(            // Títulos de pantalla (Dashboard, Nuevo protocolo)
        fontFamily = Fuente,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(           // Títulos de tarjetas y secciones
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(            // Texto normal, datos del equipo, mediciones
        fontFamily = Fuente,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(            // Texto de botones
        fontFamily = Fuente,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.3.sp
    )
)