package lol.hanyuu.iccardscanner.data.nfc

import android.nfc.tech.NfcF
import java.io.IOException

class FeliCaReader(private val nfcF: NfcF) {

    fun connect() {
        nfcF.timeout = 3000
        nfcF.connect()
    }

    fun close() {
        try { nfcF.close() } catch (_: Exception) {}
    }

    fun getIdm(): ByteArray = nfcF.tag.id

    fun getSystemCode(): Int {
        val sc = nfcF.systemCode
        return ((sc[0].toInt() and 0xFF) shl 8) or (sc[1].toInt() and 0xFF)
    }

    /**
     * READ WITHOUT ENCRYPTION (command code 06).
     * serviceCode is encoded little-endian in the frame.
     * blockIndices: 0-based block numbers to read.
     */
    fun readBlocks(serviceCode: Int, blockIndices: List<Int>): List<ByteArray> {
        val idm = getIdm()
        val blockCount = blockIndices.size
        val frameLen = 1 + 1 + 8 + 1 + 2 + 1 + blockCount * 2
        val cmd = ByteArray(frameLen)
        var pos = 0
        cmd[pos++] = frameLen.toByte()
        cmd[pos++] = 0x06                                           // command code
        idm.copyInto(cmd, pos); pos += 8
        cmd[pos++] = 0x01                                           // service count
        cmd[pos++] = (serviceCode and 0xFF).toByte()                // SC low (LE)
        cmd[pos++] = ((serviceCode ushr 8) and 0xFF).toByte()       // SC high (LE)
        cmd[pos++] = blockCount.toByte()
        for (idx in blockIndices) {
            cmd[pos++] = 0x80.toByte()  // 2-byte block descriptor, no access key
            cmd[pos++] = idx.toByte()
        }
        val response = nfcF.transceive(cmd)
        return parseReadResponse(response)
    }

    private fun parseReadResponse(resp: ByteArray): List<ByteArray> {
        if (resp.size < 13) throw IOException("Response too short: ${resp.size}")
        if (resp[1] != 0x07.toByte()) throw IOException("Unexpected code: 0x${resp[1].toInt().and(0xFF).toString(16)}")
        if (resp[10] != 0x00.toByte()) throw IOException("FeliCa status flag 1: 0x${resp[10].toInt().and(0xFF).toString(16)}")
        if (resp[11] != 0x00.toByte()) throw IOException("FeliCa status flag 2: ${resp[11]}")
        val count = resp[12].toInt() and 0xFF
        val blocks = mutableListOf<ByteArray>()
        var offset = 13
        repeat(count) {
            if (offset + 16 > resp.size) throw IOException("Truncated block data")
            blocks.add(resp.copyOfRange(offset, offset + 16))
            offset += 16
        }
        return blocks
    }
}
