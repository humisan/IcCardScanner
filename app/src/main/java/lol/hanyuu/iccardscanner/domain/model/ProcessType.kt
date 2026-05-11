package lol.hanyuu.iccardscanner.domain.model

enum class ProcessType(val displayName: String) {
    ENTRY("入出場"),
    EXIT("出場"),
    PURCHASE("物販"),
    CHARGE("チャージ"),
    OTHER("その他")
}
