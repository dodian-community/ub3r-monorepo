package net.dodian.uber.game.engine.systems.cache

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

object CacheUtils {
    fun readMedium(data: ByteArray, offset: Int): Int {
        if (offset + 2 >= data.size) {
            return 0
        }
        return ((data[offset].toInt() and 0xFF) shl 16) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            (data[offset + 2].toInt() and 0xFF)
    }

    fun readUnsignedShort(data: ByteArray, offset: Int): Int {
        if (offset + 1 >= data.size) {
            return 0
        }
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    fun hash(name: String): Int {
        var hash = 0
        val normalized = name.uppercase()
        for (index in normalized.indices) {
            hash = (hash * 61 + normalized[index].code) - 32
        }
        return hash
    }

    fun unpackBzip2(data: ByteArray): ByteArray =
        ByteArrayInputStream(BZIP_HEADER + data).use { input ->
            BZip2CompressorInputStream(input).use { compressed ->
                ByteArrayOutputStream().use { output ->
                    compressed.copyTo(output)
                    output.toByteArray()
                }
            }
        }

    fun unzipGzip(data: ByteArray): ByteArray =
        ByteArrayInputStream(data).use { input ->
            GZIPInputStream(input).use { gzip ->
                ByteArrayOutputStream().use { output ->
                    gzip.copyTo(output)
                    output.toByteArray()
                }
            }
        }

    private val BZIP_HEADER = byteArrayOf('B'.code.toByte(), 'Z'.code.toByte(), 'h'.code.toByte(), '1'.code.toByte())
}

class CacheBuffer(
    private val data: ByteArray,
) {
    var position: Int = 0
        private set

    fun seek(newPosition: Int) {
        position = newPosition.coerceIn(0, data.size)
    }

    fun skip(count: Int) {
        seek(position + count)
    }

    fun readUnsignedByte(): Int = data[position++].toInt() and 0xFF

    fun readByte(): Int = data[position++].toInt()

    fun readBoolean(): Boolean = readUnsignedByte() != 0

    fun readUnsignedShort(): Int {
        val value = CacheUtils.readUnsignedShort(data, position)
        position += 2
        return value
    }

    fun readShort(): Int {
        val value = readUnsignedShort()
        return if (value > 32767) value - 65536 else value
    }

    fun readString(): String {
        val start = position
        while (position < data.size && data[position] != 10.toByte()) {
            position++
        }
        val value = String(data, start, (position - start).coerceAtLeast(0), Charsets.ISO_8859_1)
        if (position < data.size) {
            position++
        }
        return value
    }

    fun readUnsignedSmart(): Int {
        val peek = data[position].toInt() and 0xFF
        return if (peek < 128) {
            readUnsignedByte()
        } else {
            readUnsignedShort() - 32768
        }
    }

    fun readUnsignedSmart2(): Int {
        var total = 0
        var value: Int
        do {
            value = readUnsignedSmart()
            total += value
        } while (value == 32767)
        return total
    }
}

