package lol.hanyuu.iccardscanner.domain.model

enum class ProcessType(val displayName: String) {
    ENTRY("乗車"),
    EXIT("降車"),
    PURCHASE("物販"),
    CHARGE("チャージ"),
    OTHER("その他")
}
