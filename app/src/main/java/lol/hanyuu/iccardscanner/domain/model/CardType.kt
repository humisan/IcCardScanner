package lol.hanyuu.iccardscanner.domain.model

import lol.hanyuu.iccardscanner.R

enum class CardType(
    val systemCode: Int,
    val displayName: String,
    val drawableRes: Int? = null,
) {
    SUICA(0x0003, "Suica", R.drawable.suica),
    PASMO(0x0003, "PASMO", R.drawable.pasmo),
    ICOCA(0x0003, "ICOCA", R.drawable.icoca),
    KITACA(0x0003, "Kitaca", R.drawable.kitaca),
    TOICA(0x0003, "TOICA", R.drawable.toica),
    MANACA(0x0003, "manaca", R.drawable.manaca),
    SUGOCA(0x0003, "SUGOCA", R.drawable.sugoca),
    NIMOCA(0x0003, "nimoca", R.drawable.mimoca),
    HAYAKAKEN(0x0003, "Hayakaken", R.drawable.hayasakaken),
    NANACO(0x88B4, "nanaco", R.drawable.nanaco),
    WAON(0xFE00, "WAON", R.drawable.waon),
    EDY(0x88B4, "Rakuten Edy", R.drawable.rakuten_edy),
    UNKNOWN(-1, "Unknown card", null);

    val isTransitIc: Boolean get() = systemCode == 0x0003
}
