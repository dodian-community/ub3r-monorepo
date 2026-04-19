package net.dodian.cache.util

class ByteStream(
    private val buffer: ByteArray,
) {
    private var offset = 0

    fun setOffset(newOffset: Int) {
        offset = newOffset.coerceIn(0, buffer.size)
    }

    fun length(): Int = buffer.size

    fun skip(amount: Int) {
        setOffset(offset + amount)
    }

    fun getUByte(): Int {
        if (offset >= buffer.size) {
            return 0
        }
        return buffer[offset++].toInt() and 0xFF
    }

    fun getUShort(): Int {
        val high = getUByte()
        val low = getUByte()
        return (high shl 8) or low
    }

    fun getUSmart(): Int {
        val peek = if (offset < buffer.size) buffer[offset].toInt() and 0xFF else 0
        return if (peek < 128) {
            getUByte()
        } else {
            getUShort() - 32768
        }
    }
}
