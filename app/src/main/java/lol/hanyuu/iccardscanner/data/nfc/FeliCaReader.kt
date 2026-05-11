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

    fun requestSystemCodes(): Set<Int> {
        val idm = getIdm()
        val cmd = ByteArray(REQUEST_SYSTEM_CODE_FRAME_SIZE)
        var pos = 0
        cmd[pos++] = REQUEST_SYSTEM_CODE_FRAME_SIZE.toByte()
        cmd[pos++] = REQUEST_SYSTEM_CODE_COMMAND
        idm.copyInto(cmd, pos)

        val response = nfcF.transceive(cmd)
        if (response.size < MIN_REQUEST_SYSTEM_CODE_RESPONSE_SIZE) {
            throw IOException("FeliCa system-code response too short: ${response.size}")
        }
        if (response[1] != REQUEST_SYSTEM_CODE_RESPONSE) {
            throw IOException("Unexpected FeliCa system-code response: 0x${response[1].toHex()}")
        }
        val count = response[10].toInt() and 0xFF
        val expectedSize = MIN_REQUEST_SYSTEM_CODE_RESPONSE_SIZE + count * 2
        if (response.size < expectedSize) {
            throw IOException("Truncated FeliCa system-code response: ${response.size}")
        }
        return (0 until count).mapTo(mutableSetOf()) { index ->
            val offset = 11 + index * 2
            ((response[offset].toInt() and 0xFF) shl 8) or (response[offset + 1].toInt() and 0xFF)
        }
    }

    /**
     * READ WITHOUT ENCRYPTION (command code 06).
     * serviceCode is encoded little-endian in the frame.
     * blockIndices: 0-based block numbers to read.
     */
    fun readBlocks(serviceCode: Int, blockIndices: List<Int>): List<ByteArray> {
        require(blockIndices.isNotEmpty()) { "blockIndices must not be empty" }
        require(blockIndices.size <= maxBlocksPerRead) {
            "block count must be $maxBlocksPerRead or less: ${blockIndices.size}"
        }
        require(blockIndices.all { it in 0..0xFF }) { "block index must be in 0..255" }

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
        if (resp.size < MIN_READ_RESPONSE_SIZE) {
            throw IOException("FeliCa read response too short: ${resp.size}")
        }
        if (resp[1] != READ_WITHOUT_ENCRYPTION_RESPONSE) {
            throw IOException("Unexpected FeliCa response code: 0x${resp[1].toHex()}")
        }
        val statusFlag1 = resp[10]
        val statusFlag2 = resp[11]
        if (statusFlag1 != 0x00.toByte() || statusFlag2 != 0x00.toByte()) {
            throw IOException(
                "FeliCa read failed: status1=0x${statusFlag1.toHex()}, status2=0x${statusFlag2.toHex()}"
            )
        }
        if (resp.size < MIN_SUCCESS_READ_RESPONSE_SIZE) {
            throw IOException("FeliCa read success response too short: ${resp.size}")
        }
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

    private fun Byte.toHex(): String = (toInt() and 0xFF).toString(16).padStart(2, '0')

    val maxBlocksPerRead: Int get() = MAX_BLOCKS_PER_READ

    private companion object {
        const val MAX_BLOCKS_PER_READ = 1
        const val MIN_READ_RESPONSE_SIZE = 12
        const val MIN_SUCCESS_READ_RESPONSE_SIZE = 13
        const val READ_WITHOUT_ENCRYPTION_RESPONSE: Byte = 0x07
        const val REQUEST_SYSTEM_CODE_FRAME_SIZE = 10
        const val MIN_REQUEST_SYSTEM_CODE_RESPONSE_SIZE = 11
        const val REQUEST_SYSTEM_CODE_COMMAND: Byte = 0x0C
        const val REQUEST_SYSTEM_CODE_RESPONSE: Byte = 0x0D
    }
}
