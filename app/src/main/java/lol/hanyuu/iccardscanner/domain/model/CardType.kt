package lol.hanyuu.iccardscanner.domain.model

import lol.hanyuu.iccardscanner.R

enum class CardType(
    val systemCode: Int,
    val displayName: String,
    val drawableRes: Int,
) {
    SUICA(0x0003, "Suica", R.drawable.card_suica),
    PASMO(0x0003, "PASMO", R.drawable.card_pasmo),
    ICOCA(0x0003, "ICOCA", R.drawable.card_icoca),
    KITACA(0x0003, "Kitaca", R.drawable.card_suica),
    TOICA(0x0003, "TOICA", R.drawable.card_suica),
    MANACA(0x0003, "manaca", R.drawable.card_suica),
    SUGOCA(0x0003, "SUGOCA", R.drawable.card_suica),
    NIMOCA(0x0003, "nimoca", R.drawable.card_suica),
    HAYAKAKEN(0x0003, "はやかけん", R.drawable.card_suica),
    NANACO(0x88B4, "nanaco", R.drawable.card_nanaco),
    WAON(0xFE00, "WAON", R.drawable.card_waon),
    EDY(0x88B4, "楽天Edy", R.drawable.card_edy),
    UNKNOWN(-1, "不明なカード", R.drawable.card_unknown);

    val isTransitIc: Boolean get() = systemCode == 0x0003
}
