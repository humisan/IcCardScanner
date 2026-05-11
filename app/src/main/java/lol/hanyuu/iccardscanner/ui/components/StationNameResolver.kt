package lol.hanyuu.iccardscanner.ui.components

private data class StationInfo(
    val company: String,
    val line: String,
    val station: String
) {
    val label: String get() = "$station（$line）"
}

object StationNameResolver {
    private val kansaiStations = mapOf(
        stationKey(0x8A, 0x9C) to StationInfo("京都市交通局", "烏丸線", "今出川"),
        stationKey(0x8B, 0xAC) to StationInfo("京都市交通局", "東西線", "二条"),
        stationKey(0x8B, 0xB0) to StationInfo("京都市交通局", "東西線", "太秦天神川"),
        stationKey(0xC5, 0x42) to StationInfo("京阪電気鉄道", "京阪本線", "丹波橋"),
        stationKey(0xC5, 0x02) to StationInfo("京阪電気鉄道", "京阪本線", "淀屋橋"),
        stationKey(0xC5, 0xCC) to StationInfo("京阪電気鉄道", "中之島線", "大江橋"),
        stationKey(0x81, 0x1F) to StationInfo("大阪市高速電気軌道", "御堂筋線", "淀屋橋"),
        stationKey(0x81, 0x24) to StationInfo("大阪市高速電気軌道", "御堂筋線", "なんば"),
        stationKey(0xE9, 0x07) to StationInfo("近畿日本鉄道", "京都線", "向島"),
        stationKey(0xE9, 0x1F) to StationInfo("近畿日本鉄道", "京都線", "大和西大寺"),
        stationKey(0xE9, 0x24) to StationInfo("近畿日本鉄道", "橿原線", "近鉄郡山"),
        stationKey(0x0B, 0x04) to StationInfo("西日本旅客鉄道", "片町線", "京田辺"),
    )

    fun resolve(stationCode: Int): String {
        val line = (stationCode ushr 8) and 0xFF
        val station = stationCode and 0xFF
        return kansaiStations[stationKey(line, station)]?.label ?: "駅コード ${line.toCode()}-${station.toCode()}"
    }

    fun code(stationCode: Int): String {
        val line = (stationCode ushr 8) and 0xFF
        val station = stationCode and 0xFF
        return "${line.toCode()}-${station.toCode()}"
    }
}

private fun stationKey(line: Int, station: Int): Int = (line shl 8) or station

private fun Int.toCode(): String = toString(16).padStart(2, '0').uppercase()
