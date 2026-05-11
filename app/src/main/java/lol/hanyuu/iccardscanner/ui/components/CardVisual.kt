package lol.hanyuu.iccardscanner.ui.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lol.hanyuu.iccardscanner.domain.model.CardType
import lol.hanyuu.iccardscanner.ui.theme.CardEdyBlue
import lol.hanyuu.iccardscanner.ui.theme.CardIcocaYellow
import lol.hanyuu.iccardscanner.ui.theme.CardNanacoAmber
import lol.hanyuu.iccardscanner.ui.theme.CardPasmoOrange
import lol.hanyuu.iccardscanner.ui.theme.CardSuicaGreen
import lol.hanyuu.iccardscanner.ui.theme.CardUnknownGray
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

@Composable
fun CardVisual(
    cardType: CardType,
    nickname: String,
    modifier: Modifier = Modifier
) {
    val aspectRatio = 1.586f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(brush = cardType.cardGradient())
    ) {
        val drawableRes = cardType.drawableRes
        if (drawableRes != null) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.85f
            )
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
