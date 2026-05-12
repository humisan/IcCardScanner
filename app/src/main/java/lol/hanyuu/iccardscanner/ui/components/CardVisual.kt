package lol.hanyuu.iccardscanner.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lol.hanyuu.iccardscanner.domain.model.CardType
import lol.hanyuu.iccardscanner.ui.theme.CardEdyBlue
import lol.hanyuu.iccardscanner.ui.theme.CardIcocaYellow
import lol.hanyuu.iccardscanner.ui.theme.CardNanacoAmber
import lol.hanyuu.iccardscanner.ui.theme.CardPasmoOrange
import lol.hanyuu.iccardscanner.ui.theme.CardSuicaGreen
import lol.hanyuu.iccardscanner.ui.theme.CardUnknownGray
import java.util.concurrent.ConcurrentHashMap
import lol.hanyuu.iccardscanner.ui.theme.CardWaonTeal

private fun CardType.cardGradient(): Brush = when (this) {
    CardType.SUICA, CardType.KITACA, CardType.TOICA,
    CardType.MANACA, CardType.SUGOCA, CardType.NIMOCA,
    CardType.HAYAKAKEN ->
        Brush.linearGradient(listOf(CardSuicaGreen, Color(0xFF0A4A3A)))
    CardType.PASMO ->
        Brush.linearGradient(listOf(CardPasmoOrange, Color(0xFF8B2500)))
    CardType.ICOCA ->
        Brush.linearGradient(listOf(CardIcocaYellow, Color(0xFFB5640A)))
    CardType.NANACO ->
        Brush.linearGradient(listOf(CardNanacoAmber, Color(0xFFB57800)))
    CardType.WAON ->
        Brush.linearGradient(listOf(CardWaonTeal, Color(0xFF004D40)))
    CardType.EDY ->
        Brush.linearGradient(listOf(CardEdyBlue, Color(0xFF003C8F)))
    CardType.UNKNOWN ->
        Brush.linearGradient(listOf(CardUnknownGray, Color(0xFF263238)))
}

private val transparentBitmapCache = ConcurrentHashMap<Int, Bitmap>()

@Composable
fun CardVisual(
    cardType: CardType,
    nickname: String,
    modifier: Modifier = Modifier
) {
    val aspectRatio = 1.586f
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(brush = cardType.cardGradient())
    ) {
        val drawableRes = cardType.drawableRes
        if (drawableRes != null) {
            var transparentImage by remember(drawableRes) {
                mutableStateOf(transparentBitmapCache[drawableRes])
            }
            LaunchedEffect(drawableRes) {
                if (transparentImage == null) {
                    transparentImage = withContext(Dispatchers.Default) {
                        val cached = transparentBitmapCache[drawableRes]
                        if (cached != null) {
                            cached
                        } else {
                            val decoded = decodeWithTransparentBackground(context.applicationContext, drawableRes)
                            transparentBitmapCache.putIfAbsent(drawableRes, decoded) ?: decoded
                        }
                    }
                }
            }
            transparentImage?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = cardType.displayName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = nickname,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun decodeWithTransparentBackground(context: Context, drawableRes: Int): Bitmap {
    val source = BitmapFactory.decodeResource(context.resources, drawableRes)
        ?: error("Unable to decode card image resource: $drawableRes")
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    if (source !== bitmap) source.recycle()

    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    for (index in pixels.indices) {
        val color = pixels[index]
        val alpha = color ushr 24
        val red = (color ushr 16) and 0xFF
        val green = (color ushr 8) and 0xFF
        val blue = color and 0xFF
        if (alpha > 0 && isBackgroundLike(red, green, blue)) {
            pixels[index] = color and 0x00FFFFFF
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private fun isBackgroundLike(red: Int, green: Int, blue: Int): Boolean {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    return max >= 238 && max - min <= 18
}
