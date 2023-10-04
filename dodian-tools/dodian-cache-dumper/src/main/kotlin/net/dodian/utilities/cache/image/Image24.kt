package net.dodian.utilities.cache.image

import com.displee.cache.index.archive.Archive
import com.jagex.runescape.Image24
import net.dodian.utilities.cache.cacheService
import net.dodian.utilities.cache.extensions.hashCode
import net.dodian.utilities.cache.extensions.toByteBuf
import net.dodian.utilities.cache.services.CacheService
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/*
object Image24Loader {

    fun load(media: Archive, name: String, id: Int): Image24 {
        val uid = (name.hashCode shl 8) + id.toLong()

        val dat = media.file("${name}.dat")?.data?.toByteBuf()
            ?: error("Couldn't load file '${name}.dat'")
        val idx = media.file("index.dat")?.data?.toByteBuf()
            ?: error("Couldn't load file 'index.dat'")

        idx.readerIndex(dat.readUnsignedShort())
        val cropW = idx.readUnsignedShort()
        val cropH = idx.readUnsignedShort()

        val paletteSize = idx.readUnsignedByte().toInt()
        val palette = IntArray(paletteSize)
        for (k in 0 until paletteSize - 1) {
            palette[k + 1] = idx.readMedium()
            if (palette[k + 1] == 0)
                palette[k + 1] = 1
        }
        for (i in 0 until id) {
            idx.readerIndex(idx.readerIndex() + 2)
            dat.readerIndex(dat.readerIndex() + idx.readUnsignedShort() * idx.readUnsignedShort())
            idx.readerIndex(idx.readerIndex() - 1)
        }
        val cropX = idx.readUnsignedByte().toInt()
        val cropY = idx.readUnsignedByte().toInt()
        val width = idx.readUnsignedShort()
        val height = idx.readUnsignedShort()

        val layout = idx.readUnsignedByte().toInt()
        val pixelLength = width * height
        val pixels = IntArray(pixelLength)
        if (layout == 0) {
            for (i in 0 until pixelLength)
                pixels[i] = palette[dat.readUnsignedByte().toInt()]
        } else if (layout == 1) {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    pixels[x + (y * width)] = palette[dat.readUnsignedByte().toInt()]
                }
            }
        }

        return Image24(
            pixels = pixels.toList(),
            width = width,
            height = height,
            cropX = cropX,
            cropY = cropY,
            cropW = cropW,
            cropH = cropH
        )
    }
}

data class Image24(
    val pixels: List<Int>,
    val width: Int,
    val height: Int,
    val cropX: Int,
    val cropY: Int,
    val cropW: Int,
    val cropH: Int
)

*/
fun main() {
    cacheService = CacheService("./game-utilities/game-cache/data/cache_317")

    val id  = 0
    val name = "staticons"

    val uid = (name.hashCode shl 8) + id

    val image = Image24(cacheService.mediaArchive, name, id)

    val imageBytes = ByteArrayOutputStream()
    ImageIO.write(image.toBufferedImage(), "png", File("./game-utilities/game-cache/data/dumps/${name}_${id}.png"))

    //val image24 = Image24Loader.load(cacheService.mediaArchive, "staticons", 0)
}
