package lol.hanyuu.iccardscanner.ui.components

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class StationLabel(
    val code: String,
    val name: String,
    val line: String,
    val company: String
) {
    val displayName: String get() = "$name（$line）"
}

object StationNameResolver {
    @Volatile
    private var cachedStations: Map<Int, StationLabel>? = null

    fun resolve(context: Context, areaCode: Int?, stationCode: Int): StationLabel {
        val line = (stationCode ushr 8) and 0xFF
        val station = stationCode and 0xFF
        val fallback = StationLabel(
            code = "${line.toCode()}-${station.toCode()}",
            name = "駅コード ${line.toCode()}-${station.toCode()}",
            line = "不明",
            company = "不明"
        )
        val area = areaCode ?: return fallback
        return loadStations(context)[stationKey(area, line, station)] ?: fallback
    }

    private fun loadStations(context: Context): Map<Int, StationLabel> =
        cachedStations ?: synchronized(this) {
            cachedStations ?: readStations(context).also { cachedStations = it }
        }

    private fun readStations(context: Context): Map<Int, StationLabel> =
        context.assets.open("station_codes.csv").use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                lines.drop(1).mapNotNull(::parseStationLine).associateBy { it.first }.mapValues { it.value.second }
            }
        }

    private fun parseStationLine(line: String): Pair<Int, StationLabel>? {
        val columns = line.split(',')
        if (columns.size < 6) return null
        val area = columns[0].toIntOrNull(16) ?: return null
        val lineCode = columns[1].toIntOrNull(16) ?: return null
        val stationCode = columns[2].toIntOrNull(16) ?: return null
        val company = columns[3].ifBlank { "不明" }
        val lineName = columns[4].ifBlank { "不明" }
        val stationName = columns[5].ifBlank { return null }
        val code = "${lineCode.toCode()}-${stationCode.toCode()}"
        return stationKey(area, lineCode, stationCode) to StationLabel(
            code = code,
            name = stationName,
            line = lineName,
            company = company
        )
    }

    private fun stationKey(area: Int, line: Int, station: Int): Int =
        (area shl 16) or (line shl 8) or station
}

private fun Int.toCode(): String = toString(16).padStart(2, '0').uppercase()
