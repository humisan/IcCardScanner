package lol.hanyuu.iccardscanner.domain.model

enum class ProcessType(val displayName: String) {
    ENTRY("入場"),
    EXIT("退場"),
    PURCHASE("物販"),
    CHARGE("チャージ"),
    OTHER("その他")
}
