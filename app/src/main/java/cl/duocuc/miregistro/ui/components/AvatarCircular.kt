package cl.duocuc.miregistro.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme

@Composable
fun AvatarCircular(
    @DrawableRes imagen: Int,
    descripcion: String,
    modifier: Modifier = Modifier,
    // NUEVO (Avatar): foto del usuario leída de la BD. Si es null se dibuja la imagen por
    // defecto (imagen); así los usos y @Preview anteriores no cambian.
    foto: ImageBitmap? = null
) {
    Image(
        painter = if (foto != null) BitmapPainter(foto) else painterResource(id = imagen),
        contentDescription = descripcion,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(Dimens.tamanoAvatar)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .border(Dimens.bordeAvatar, MaterialTheme.colorScheme.surface, CircleShape)
    )
}

@Preview(showBackground = true)
@Composable
private fun AvatarCircularPreview() {
    ProtoFieldTheme {
        AvatarCircular(imagen = R.drawable.avatar_usuario, descripcion = "", modifier = Modifier.padding(16.dp))
    }
}
