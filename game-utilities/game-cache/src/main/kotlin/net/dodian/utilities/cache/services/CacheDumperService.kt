package net.dodian.utilities.cache.services

import com.github.michaelbull.logging.InlineLogger
import com.jagex.runescape.Image24
import com.jagex.runescape.Image8
import net.dodian.utilities.cache.dumpToJson
import net.dodian.utilities.cache.extensions.toByteBuf
import net.dodian.utilities.cache.types.*
import net.dodian.utilities.cache.types.iftype.Image24Def
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

private val logger = InlineLogger()

class CacheDumperService(private val cacheService: CacheService) {

    init {
        mapOf(
            "flo" to FloTypeLoader,
            "idk" to IdkTypeLoader,
            "loc" to LocTypeLoader,
            "npc" to NpcTypeLoader,
            "obj" to ObjTypeLoader,
            "seq" to SeqTypeLoader,
            "spotanim" to SpotAnimTypeLoader,
            "varbit" to VarbitTypeLoader,
            "varp" to VarpTypeLoader,
        ).dumpToJson(cacheService)

        dumpMedia()
        dumpTitle()
    }

    private fun dumpTitle() {
        val title = cacheService.titleArchive

        val images = mutableMapOf<String, BufferedImage>()

        listOf(
            "logo",
            "titlebox",
            "titlebutton"
        ).forEach {
            images[it] = Image24(
                title,
                it.split("-").first(),
                it.split("-").last().toIntOrNull() ?: 0
            ).toBufferedImage()
        }

        for (i in 0 until 12) {
            images["runes-$i"] = Image24(title, "runes", i).toBufferedImage()
            images["runes-${12 + (i and 3)}"] = Image24(title, "runes", 12 + (i and 3)).toBufferedImage()
        }

        val imageDumps = Path(cacheService.path).resolve("dumps/images").resolve("title")

        if (imageDumps.notExists())
            imageDumps.createDirectories()

        images.forEach { (name, buffer) ->
            val path = imageDumps.resolve(name.split("-").joinToString("/") + ".png")
            if (path.notExists())
                path.createDirectories()

            ImageIO.write(buffer, "png", path.toFile())
        }
    }

    private fun dumpMedia() {
        val media = cacheService.mediaArchive

        val imageDumps = Path(cacheService.path).resolve("dumps/images").resolve("2d graphics")

        if (imageDumps.notExists())
            imageDumps.createDirectories()

        val images = mutableMapOf<String, BufferedImage>()

        listOf(
            "invback",
            "chatback",
            "backbase1",
            "backbase2",
            "backhmid1",
            "compass",
            "mapedge",
            "mapback",
            "mapmarker-0",
            "mapmarker-1",
            "mapdots-0",
            "mapdots-1",
            "mapdots-2",
            "mapdots-3",
            "mapdots-4",
            "scrollbar-0",
            "scrollbar-1",
            "redstone1",
            "redstone2",
            "redstone3",
            "mod_icons-0",
            "mod_icons-1",
            "backleft1",
            "backleft2",
            "backright1",
            "backright2",
            "backtop1",
            "backvmid1",
            "backvmid2",
            "backvmid3",
            "backhmid2"
        ).forEach {
            images[it] = Image24(
                media,
                it.split("-").first(),
                it.split("-").last().toIntOrNull() ?: 0
            ).toBufferedImage()
        }

        for (i in 0 until 13) {
            images["sideicons-$i"] = Image24(media, "sideicons", i).toBufferedImage()
        }

        for (i in 0 until 56) {
            images["mapscene-$i"] = Image24(media, "mapscene", i).toBufferedImage()
        }

        for (i in 0 until 57) {
            images["mapfunction-$i"] = Image24(media, "mapfunction", i).toBufferedImage()
        }

        for (i in 0 until 5) {
            images["hitmarks-$i"] = Image24(media, "hitmarks", i).toBufferedImage()
        }

        for (i in 0 until 10) {
            images["headicons-$i"] = Image24(media, "headicons", i).toBufferedImage()
        }

        for (i in 0 until 8) {
            images["cross-$i"] = Image24(media, "cross", i).toBufferedImage()
        }

        cacheService.ifTypes.filter { it.image != null || it.activeImage == null || it.inventorySlotImage != null }
            .forEach {
                it.image?.addTo(images)
                it.activeImage?.addTo(images)
                it.inventorySlotImage?.addTo(images)
            }

        images.forEach { (name, buffer) ->
            val path = imageDumps.resolve(name.split("-").joinToString("/") + ".png")
            if (path.notExists())
                path.createDirectories()

            ImageIO.write(buffer, "png", path.toFile())
        }
    }

    private fun Image24Def.addTo(images: MutableMap<String, BufferedImage>) {
        images["$name-$id"] = Image24(cacheService.mediaArchive, name, id).toBufferedImage()
    }
}

fun main() {
    CacheDumperService(CacheService())
}