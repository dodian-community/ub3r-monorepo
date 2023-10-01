package net.dodian.utilities.cache.fonts

import com.displee.cache.CacheLibrary
import net.dodian.utilities.cache.extensions.toByteBuf
import net.dodian.utilities.cache.services.CacheService

data class BitmapFont(
    val charMask: MutableList<ByteArray> = Array(256) { ByteArray(0) }.toMutableList(),
    val charMaskWidth: MutableList<Int> = Array(256) { 0 }.toMutableList(),
    val charMaskHeight: MutableList<Int> = Array(256) { 0 }.toMutableList(),
    val charOffsetX: MutableList<Int> = Array(256) { 0 }.toMutableList(),
    val charOffsetY: MutableList<Int> = Array(256) { 0 }.toMutableList(),
    val charAdvance: MutableList<Int> = Array(256) { 0 }.toMutableList(),
    var height: Int = 0
)

object FontLoader {

    fun load(cache: CacheLibrary, fontName: String, quill: Boolean = false): BitmapFont {
        val buffer = cache.data(0, 1, "$fontName.dat")?.toByteBuf()
            ?: error("Unable to read title...")
        val meta = cache.data(0, 1, "index.dat")?.toByteBuf()
            ?: error("Unable to read index.dat")

        meta.readerIndex(buffer.readUnsignedShort() + 4)

        val k = meta.readUnsignedByte().toInt()
        if (k > 0)
            meta.readerIndex(meta.readerIndex() + 3 * (k - 1))

        with (BitmapFont()) {
            for (c in 0 until 256) {
                charOffsetX[c] = meta.readUnsignedByte().toInt()
                charOffsetY[c] = meta.readUnsignedByte().toInt()

                val w = meta.readUnsignedShort()
                val h = meta.readUnsignedShort()
                charMaskWidth[c] = w
                charMaskHeight[c] = h

                val storeOrder = meta.readUnsignedByte().toInt()

                val length = w * h
                charMask[c] = ByteArray(length)

                if (storeOrder == 0) {
                    for (i in 0 until length)
                        charMask[c][i] = buffer.readByte()
                } else if (storeOrder == 1) {
                    for (x in 0 until w) {
                        for (y in 0 until h) {
                            charMask[c][x + (y * w)] = buffer.readByte()
                        }
                    }
                }

                if (h > height && c < 128)
                    height = h

                charOffsetX[c] = 1
                charAdvance[c] = w + 2

                var acc = 0
                for (y in h / 7 until h)
                    acc += charMask[c][y * w]

                if (acc <= (h / 7)) {
                    charAdvance[c]--
                    charOffsetX[c] = 0
                }

                acc = 0

                for (y in h / 7 until h)
                    acc += charMask[c][(w - 1) + (y * w)]

                if (acc <= (h / 7))
                    charAdvance[c]--
            }

            return this
        }
    }
}

fun main() {
    val service = CacheService("./game-utilities/game-cache/data/cache_317")
}