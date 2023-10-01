package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.utilities.cache.extensions.*

private val logger = InlineLogger()

data class LocType(
    val id: Int,
    val name: String,
    val examine: String,
    val important: Boolean,
    val lightAmbient: Byte,
    val lightAttenuation: Byte,
    val translateX: Int,
    val translateY: Int,
    val translateZ: Int,
    val scaleX: Int,
    val scaleY: Int,
    val scaleZ: Int,
    val sizeX: Int,
    val sizeZ: Int,
    val varp: Int,
    val srcColor: List<Int>,
    val dstColor: List<Int>,
    val mapFunctionIcon: Int,
    val mapSceneIcon: Int,
    val invert: Boolean,
    val blocksProjectiles: Boolean,
    val overrideTypeIds: List<Int>,
    val supportsObj: Int,
    val adjustToTerrain: Boolean,
    val occludes: Boolean,
    val decorative: Boolean,
    val solid: Boolean,
    val interactionSideFlags: Int,
    val dynamic: Boolean,
    val modelIds: List<Int>,
    val varbit: Int,
    val decorOffset: Int,
    val modelKinds: List<Int>,
    val interactAble: Boolean,
    val castShadow: Boolean,
    val seqId: Int,
    val options: List<String?>
) : Type

data class LocTypeBuilder(
    var id: Int,
    var name: String? = null,
    var examine: String = "I don't know anything about this object...",
    var important: Boolean = false,
    var lightAmbient: Byte = 0,
    var lightAttenuation: Byte = 0,
    var translateX: Int = 0,
    var translateY: Int = 0,
    var translateZ: Int = 0,
    var scaleX: Int = 128,
    var scaleY: Int = 128,
    var scaleZ: Int = 128,
    var sizeX: Int = 1,
    var sizeZ: Int = 1,
    var seqId: Int = -1,
    var varp: Int = -1,
    var varbit: Int = -1,
    var mapSceneIcon: Int = -1,
    var mapFunctionIcon: Int = -1,
    var decorOffset: Int = 16,
    var supportsObj: Int = -1,
    var interactionSideFlags: Int = 0,
    var solid: Boolean = true,
    var blocksProjectiles: Boolean = true,
    var invert: Boolean = false,
    var dynamic: Boolean = false,
    var occludes: Boolean = false,
    var decorative: Boolean = false,
    var castShadow: Boolean = true,
    var adjustToTerrain: Boolean = false,
    var interactAble: Boolean? = null,
    var modelKinds: MutableList<Int> = mutableListOf(),
    var overrideTypeIds: MutableList<Int> = mutableListOf(),
    var modelIds: MutableList<Int> = mutableListOf(),
    var srcColor: MutableList<Int> = mutableListOf(),
    var dstColor: MutableList<Int> = mutableListOf(),
    var options: MutableList<String?> = mutableListOf()
) : TypeBuilder<LocType> {

    override fun build() = LocType(
        id = id,
        name = name ?: "null",
        examine = examine,
        important = important,
        lightAmbient = lightAmbient,
        lightAttenuation = lightAttenuation,
        translateX = translateX,
        translateY = translateY,
        translateZ = translateZ,
        scaleX = scaleX,
        scaleY = scaleY,
        scaleZ = scaleZ,
        sizeX = sizeX,
        sizeZ = sizeZ,
        varp = varp,
        mapFunctionIcon = mapFunctionIcon,
        mapSceneIcon = mapSceneIcon,
        invert = invert,
        blocksProjectiles = blocksProjectiles,
        supportsObj = supportsObj,
        adjustToTerrain = adjustToTerrain,
        occludes = occludes,
        decorative = decorative,
        solid = solid,
        interactionSideFlags = interactionSideFlags,
        dynamic = dynamic,
        varbit = varbit,
        decorOffset = decorOffset,
        interactAble = interactAble ?: error("No value set for 'interactAble'"),
        castShadow = castShadow,
        seqId = seqId,
        modelKinds = modelKinds,
        overrideTypeIds = overrideTypeIds,
        modelIds = modelIds,
        srcColor = srcColor,
        dstColor = dstColor,
        options = options
    )
}

object LocTypeLoader : TypeLoader<LocType> {

    override fun load(cache: CacheLibrary): List<LocType> {
        val types = mutableListOf<LocType>()

        val data = cache.typeBuffer(ConfigType.Loc)
        val meta = cache.typeMeta(ConfigType.Loc)

        val count = meta.readUnsignedShort()
        val indices = IntArray(count)

        logger.info { "Loading $count LocTypes..." }

        var index = ARCHIVE_CONFIG
        for (i in 0 until count) {
            indices[i] = index
            index += meta.readUnsignedShort()
        }

        for (typeId in 0 until count) {
            data.readerIndex(indices[typeId])
            types += readType(data, typeId)
        }

        logger.info { "Loaded $count LocTypes..." }
        println()
        return types
    }

    private fun readType(buf: ByteBuf, id: Int): LocType {
        val builder = LocTypeBuilder(id)

        var reading = true
        while (reading)
            reading = readBuffer(buf, builder, buf.readUnsignedByte().toInt())

        builder.apply {
            if (interactAble == null) {
                interactAble = (modelIds.isNotEmpty() && (modelKinds.isEmpty() || modelKinds[0] == 10))

                if (options.isNotEmpty())
                    interactAble = true
            }

            if (decorative) {
                solid = false
                blocksProjectiles = false
            }

            if (supportsObj == -1)
                supportsObj = if (solid) 1 else 0
        }

        return builder.build()
    }

    private fun readBuffer(buf: ByteBuf, builder: LocTypeBuilder, instruction: Int) = with(builder) {
        //logger.debug { "Decoding instruction: $instruction, for Loc ${builder.id}" }

        when (instruction) {
            1 -> {
                val count = buf.readUnsignedByte().toInt()
                if (count <= 0) return@with true

                if ((modelIds.isNotEmpty())) {
                    buf.readerIndex(buf.readerIndex() + (count * 3))
                    return@with true
                }

                modelKinds = IntArray(count).toMutableList()
                modelIds = IntArray(count).toMutableList()

                for (kind in 0 until count) {
                    modelIds[kind] = buf.readUnsignedShort()
                    modelKinds[kind] = buf.readUnsignedByte().toInt()
                }
            }

            2 -> name = buf.readString()
            3 -> examine = buf.readString()
            5 -> {
                val count = buf.readUnsignedByte().toInt()
                if (count <= 0) return@with true

                if (modelIds.isNotEmpty()) {
                    buf.readerIndex(buf.readerIndex() + (count * 2))
                    return@with true
                }

                modelKinds = mutableListOf()
                modelIds = IntArray(count).toMutableList()
                for (model in 0 until count)
                    modelIds[model] = buf.readUnsignedShort()
            }

            14 -> sizeX = buf.readUnsignedByte().toInt()
            15 -> sizeZ = buf.readUnsignedByte().toInt()
            17 -> solid = false
            18 -> blocksProjectiles = false
            19 -> interactAble = buf.readUnsignedByte().toInt() == 1
            21 -> adjustToTerrain = true
            22 -> dynamic = true
            23 -> occludes = true
            24 -> {
                seqId = buf.readUnsignedShort()
                if (seqId == 65535) seqId = -1
            }

            28 -> decorOffset = buf.readUnsignedByte().toInt()
            29 -> lightAmbient = buf.readByte()
            39 -> lightAttenuation = buf.readByte()
            in 30 until 39 -> {
                if (options.isEmpty())
                    options = Array<String?>(5) { null }.toMutableList()

                options[instruction - 30] = buf.readString()
                if (options[instruction - 30].equals("hidden", true))
                    options[instruction - 30] = null
            }

            40 -> {
                val count = buf.readUnsignedByte().toInt()
                srcColor = IntArray(count).toMutableList()
                dstColor = IntArray(count).toMutableList()
                for (i in 0 until count) {
                    srcColor[i] = buf.readUnsignedShort()
                    dstColor[i] = buf.readUnsignedShort()
                }
            }

            60 -> mapFunctionIcon = buf.readUnsignedShort()
            62 -> invert = true
            64 -> castShadow = false
            65 -> scaleX = buf.readUnsignedShort()
            66 -> scaleZ = buf.readUnsignedShort()
            67 -> scaleY = buf.readUnsignedShort()
            68 -> mapSceneIcon = buf.readUnsignedShort()
            69 -> interactionSideFlags = buf.readUnsignedByte().toInt()
            70 -> translateX = buf.readShort().toInt()
            71 -> translateY = buf.readShort().toInt()
            72 -> translateZ = buf.readShort().toInt()
            73 -> important = true
            74 -> decorative = true
            75 -> supportsObj = buf.readUnsignedByte().toInt()
            77 -> {
                val varbit = buf.readUnsignedShort()
                this.varbit = if (varbit == 65535) -1 else varbit

                val varp = buf.readUnsignedShort()
                this.varp = if (varp == 65535) -1 else varp

                val count = buf.readUnsignedByte().toInt()
                overrideTypeIds = IntArray(count + 1).toMutableList()
                for (i in 0..count) {
                    overrideTypeIds[i] = buf.readUnsignedShort()
                    if (overrideTypeIds[i] == 65535)
                        overrideTypeIds[i] = -1
                }
            }

            0 -> return@with false
            else -> {
                logger.debug { builder }
                error("Unrecognized instruction: $instruction")
            }
        }

        return@with true
    }
}