package net.dodian.utilities.cache.types.iftype

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import net.dodian.utilities.cache.extensions.RGBColor
import net.dodian.utilities.cache.extensions.asRGB
import net.dodian.utilities.cache.extensions.readString
import net.dodian.utilities.cache.extensions.toByteBuf
import net.dodian.utilities.cache.fonts.BitmapFont
import net.dodian.utilities.cache.types.Type
import net.dodian.utilities.cache.types.TypeBuilder

private val logger = InlineLogger()

object IfTypeLoader {

    fun load(cache: CacheLibrary, fonts: List<BitmapFont>): List<IfType> {
        val types = mutableListOf<IfType>()

        val buffer = cache.data(0, 3, "data")?.toByteBuf()
            ?: error("Unable to read interfaces...")

        val count = buffer.readUnsignedShort()
        logger.info { "Loading $count IfTypes..." }

        var id: Int
        var parentId = -1
        while (buffer.readerIndex() < buffer.array().size) {
            id = buffer.readUnsignedShort()

            if (id == 65535) {
                parentId = buffer.readUnsignedShort()
                id = buffer.readUnsignedShort()
            }

            val builder = IfTypeBuilder()
            builder.id = id
            builder.parentId = parentId
            builder.type = buffer.readUnsignedByte().toInt()
            builder.optionType = buffer.readUnsignedByte().toInt()
            builder.contentType = buffer.readUnsignedShort()
            builder.width = buffer.readUnsignedShort()
            builder.height = buffer.readUnsignedShort()
            builder.transparency = buffer.readUnsignedByte().toByte()
            builder.delegateHover = buffer.readUnsignedByte().toInt()

            // println()
            // logger.debug { "Buffer (${buffer.readerIndex()}/${buffer.array().size})" }
            // logger.debug { "[${builder.type}] Decoding $id, with parent $parentId" }

            if (builder.delegateHover != 0) {
                builder.delegateHover = ((builder.delegateHover - 1) shl 8) + buffer.readUnsignedByte()
            } else {
                builder.delegateHover = -1
            }

            val comparatorCount = buffer.readUnsignedByte().toInt()
            if (comparatorCount > 0) {
                builder.scriptComparator = IntArray(comparatorCount).toMutableList()
                builder.scriptOperand = IntArray(comparatorCount).toMutableList()
                for (i in 0 until comparatorCount) {
                    builder.scriptComparator[i] = buffer.readUnsignedByte().toInt()
                    builder.scriptOperand[i] = buffer.readUnsignedShortLE()
                }
            }

            val scriptCount = buffer.readUnsignedByte().toInt()
            if (scriptCount > 0) {
                for (scriptId in 0 until scriptCount) {
                    val length = buffer.readUnsignedShort()
                    builder.scripts.add(scriptId, IntArray(length).toMutableList())
                    for (i in 0 until length)
                        builder.scripts[scriptId].add(i, buffer.readUnsignedShort())
                }
            }

            val type = IfTypes.entries.getOrNull(builder.type)

            if (type == IfTypes.PARENT) {
                builder.scrollableHeight = buffer.readUnsignedShort()
                builder.hide = buffer.readUnsignedByte().toInt() == 1
                val childCount = buffer.readUnsignedShort()
                builder.childId = IntArray(childCount).toMutableList()
                builder.childX = IntArray(childCount).toMutableList()
                builder.childY = IntArray(childCount).toMutableList()
                for (i in 0 until childCount) {
                    builder.childId[i] = buffer.readUnsignedShort()
                    builder.childX[i] = buffer.readShort().toInt()
                    builder.childY[i] = buffer.readShort().toInt()
                }
            }

            if (type == IfTypes.UNUSED) {
                buffer.readUnsignedShort()
                buffer.readUnsignedByte()
            }

            if (type == IfTypes.INVENTORY) {
                builder.inventorySlotObjId = IntArray(builder.width * builder.height).toMutableList()
                builder.inventorySlotObjCount = IntArray(builder.width * builder.height).toMutableList()
                builder.inventoryDraggable = buffer.readUnsignedByte().toInt() == 1
                builder.inventoryInteractAble = buffer.readUnsignedByte().toInt() == 1
                builder.inventoryUsable = buffer.readUnsignedByte().toInt() == 1
                builder.inventoryMoveReplaces = buffer.readUnsignedByte().toInt() == 1
                builder.inventoryMarginX = buffer.readUnsignedByte().toInt()
                builder.inventoryMarginY = buffer.readUnsignedByte().toInt()

                builder.inventorySlotOffsetX = IntArray(20).toMutableList()
                builder.inventorySlotOffsetY = IntArray(20).toMutableList()
                for (slot in 0 until 20) {
                    if (buffer.readUnsignedByte().toInt() != 1)
                        continue

                    builder.inventorySlotOffsetX[slot] = buffer.readShort().toInt()
                    builder.inventorySlotOffsetY[slot] = buffer.readShort().toInt()
                    val imageName = buffer.readString()
                    //logger.debug { "[Inventory] ${builder.id}: $imageName" }
                }

                builder.inventoryOptions = Array<String>(5) { "" }.toMutableList()
                for (i in 0 until 5)
                    builder.inventoryOptions[i] = buffer.readString()
            }

            if (type == IfTypes.RECT)
                builder.fill = buffer.readUnsignedByte().toInt() == 1

            if (type == IfTypes.TEXT || type == IfTypes.UNUSED) {
                builder.center = buffer.readUnsignedByte().toInt() == 1
                val fontId = buffer.readUnsignedByte()
                // TODO:
                //logger.debug { "[Text/Font] Font ID: $fontId" }
                builder.shadow = buffer.readUnsignedByte().toInt() == 1
            }

            if (type == IfTypes.TEXT) {
                builder.text = buffer.readString()
                builder.activeText = buffer.readString()
            }

            if (type == IfTypes.UNUSED || type == IfTypes.RECT || type == IfTypes.TEXT)
                builder.color = buffer.readInt().asRGB

            if (type == IfTypes.RECT || type == IfTypes.TEXT) {
                builder.activeColor = buffer.readInt().asRGB
                builder.hoverColor = buffer.readInt().asRGB
                builder.activeHoverColor = buffer.readInt().asRGB
            }

            if (type == IfTypes.IMAGE) {
                val imageName = buffer.readString()
                val activeImageName = buffer.readString()

                if (imageName.contains(",")) {
                    val parts = imageName.split(",")
                    builder.image = Image24Def(name = parts[0], id = parts[1].toInt())
                }

                if (activeImageName.contains(",")) {
                    val parts = activeImageName.split(",")
                    builder.activeImage = Image24Def(name = parts[0], id = parts[1].toInt())
                }
            }

            if (type == IfTypes.MODEL) {
                var tmp = buffer.readUnsignedByte().toInt()
                if (tmp != 0) {
                    builder.modelType = IfModelTypes.NORMAL.ordinal
                    builder.modelId = ((tmp - 1) shl 8) + buffer.readUnsignedByte()
                }

                tmp = buffer.readUnsignedByte().toInt()
                if (tmp != 0) {
                    builder.activeModelType = IfModelTypes.NORMAL.ordinal
                    builder.activeModelId = ((tmp - 1) shl 8) + buffer.readUnsignedByte()
                }

                tmp = buffer.readUnsignedByte().toInt()
                if (tmp != 0) {
                    builder.seqId = ((tmp - 1) shl 8) + buffer.readUnsignedByte()
                } else builder.seqId = -1

                tmp = buffer.readUnsignedByte().toInt()
                if (tmp != 0) {
                    builder.activeSeqId = ((tmp - 1) shl 8) + buffer.readUnsignedByte()
                } else builder.activeSeqId = -1

                builder.modelZoom = buffer.readUnsignedShort()
                builder.modelPitch = buffer.readUnsignedShort()
                builder.modelYaw = buffer.readUnsignedShort()
            }

            if (type == IfTypes.INVENTORY_TEXT) {
                builder.inventorySlotObjId = IntArray(builder.width * builder.height).toMutableList()
                builder.inventorySlotObjCount = IntArray(builder.width * builder.height).toMutableList()
                builder.center = buffer.readUnsignedByte().toInt() == 1
                val fontId = buffer.readUnsignedByte()
                // TODO:
                //logger.debug { "[Inventory Text] Font ID: $fontId" }
                builder.shadow = buffer.readUnsignedByte().toInt() == 1
                builder.color = buffer.readInt().asRGB
                builder.inventoryMarginX = buffer.readShort().toInt()
                builder.inventoryMarginY = buffer.readShort().toInt()
                builder.inventoryInteractAble = buffer.readUnsignedByte().toInt() == 1
                builder.inventoryOptions = Array<String>(5) { "" }.toMutableList()
                for (option in 0 until 5)
                    builder.inventoryOptions[option] = buffer.readString()
            }

            if (type == IfTypes.SPELL)
                builder.spellAction = buffer.readString()

            if (builder.optionType == IfOptionTypes.SPELL.ordinal + 1 || type == IfTypes.INVENTORY) {
                builder.spellAction = buffer.readString()
                builder.spellName = buffer.readString()
                builder.spellFlags = buffer.readUnsignedShort()
            }

            if (builder.option == null && listOf(
                    IfOptionTypes.STANDARD,
                    IfOptionTypes.CONTINUE,
                    IfOptionTypes.SELECT,
                    IfOptionTypes.TOGGLE
                ).contains(IfOptionTypes.entries.singleOrNull { it.ordinal + 1 == builder.optionType })
            ) builder.option = buffer.readString()

            types += builder.build()
        }

        logger.info { "Loaded $count IfTypes..." }
        println()
        return types
    }
}


data class IfType(
    val id: Int,
    val parentId: Int,
    val type: Int,
    var x: Int,
    var y: Int,
    val width: Int,
    val height: Int,
    val seqId: Int,
    val seqFrame: Int,
    val seqCycle: Int,
    val transparency: Byte,
    val text: String?,
    val shadow: Boolean,
    val spellAction: String?,
    val spellName: String?,
    val spellFlags: Int,
    val scrollPosition: Int,
    val scrollableHeight: Int,
    val scripts: List<List<Int>>,
    val scriptOperand: List<Int>,
    val scriptComparator: List<Int>,
    val optionType: Int,
    val option: String?,
    val modelId: Int,
    val modelType: Int,
    val modelZoom: Int,
    val modelPitch: Int,
    val modelYaw: Int,
    val inventoryUsable: Boolean,
    val inventorySlotOffsetX: List<Int>,
    val inventorySlotOffsetY: List<Int>,
    val inventorySlotObjId: List<Int>,
    val inventorySlotObjCount: List<Int>,
    val inventoryOptions: List<String>,
    val inventoryMoveReplaces: Boolean,
    val inventoryMarginX: Int,
    val inventoryMarginY: Int,
    val inventoryDraggable: Boolean,
    val image: Image24Def?,
    val activeImage: Image24Def?,
    val inventorySlotImage: Image24Def?,
    val hide: Boolean,
    // TODO: font
    val fill: Boolean,
    val delegateHover: Int,
    val contentType: Int,
    val childX: List<Int>,
    val childY: List<Int>,
    val childId: List<Int>,
    val center: Boolean,
    val activeText: String?,
    val activeSeqId: Int,
    val activeModelId: Int,
    val activeModelType: Int,
    val inventoryInteractAble: Boolean,
    val activeHoverColor: RGBColor,
    val activeColor: RGBColor,
    val hoverColor: RGBColor,
    val color: RGBColor,
) : Type

data class Image24Def(
    val name: String,
    val id: Int
)

data class IfTypeBuilder(
    var id: Int? = null,
    var parentId: Int = -1,
    var type: Int = 0,
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var seqId: Int = 0,
    var seqFrame: Int = 0,
    var seqCycle: Int = 0,
    var transparency: Byte = 0,
    var text: String? = null,
    var shadow: Boolean = false,
    var spellAction: String? = null,
    var spellName: String? = null,
    var spellFlags: Int = 0,
    var scrollPosition: Int = 0,
    var scrollableHeight: Int = 0,
    var scripts: MutableList<MutableList<Int>> = mutableListOf(),
    var scriptOperand: MutableList<Int> = mutableListOf(),
    var scriptComparator: MutableList<Int> = mutableListOf(),
    var optionType: Int = 0,
    var option: String? = null,
    var modelId: Int = 0,
    var modelType: Int = 0,
    var modelZoom: Int = 0,
    var modelPitch: Int = 0,
    var modelYaw: Int = 0,
    var inventoryUsable: Boolean = false,
    var inventorySlotOffsetX: MutableList<Int> = mutableListOf(),
    var inventorySlotOffsetY: MutableList<Int> = mutableListOf(),
    var inventorySlotObjId: MutableList<Int> = mutableListOf(),
    var inventorySlotObjCount: MutableList<Int> = mutableListOf(),
    var inventoryOptions: MutableList<String> = mutableListOf(),
    var inventoryMoveReplaces: Boolean = false,
    var inventoryMarginX: Int = 0,
    var inventoryMarginY: Int = 0,
    var inventoryDraggable: Boolean = false,
    var hoverColor: RGBColor = RGBColor(),
    var hide: Boolean = false,
    var fill: Boolean = false,
    var delegateHover: Int = 0,
    var contentType: Int = 0,
    var color: RGBColor = RGBColor(),
    var childX: MutableList<Int> = mutableListOf(),
    var childY: MutableList<Int> = mutableListOf(),
    var childId: MutableList<Int> = mutableListOf(),
    var center: Boolean = false,
    var activeText: String? = null,
    var activeSeqId: Int = -1,
    var activeModelId: Int = -1,
    var activeModelType: Int = -1,
    var activeHoverColor: RGBColor = RGBColor(),
    var activeColor: RGBColor = RGBColor(),
    var inventoryInteractAble: Boolean = false,
    var image: Image24Def? = null,
    var activeImage: Image24Def? = null,
    var inventorySlotImage: Image24Def? = null,
    // TODO: font
) : TypeBuilder<IfType> {

    override fun build() = IfType(
        id = id ?: error("No value defined for 'id'"),
        parentId = parentId,
        type = type,
        x = x,
        y = y,
        width = width,
        height = height,
        seqId = seqId,
        seqFrame = seqFrame,
        seqCycle = seqCycle,
        transparency = transparency,
        text = text,
        shadow = shadow,
        spellAction = spellAction,
        spellName = spellName,
        spellFlags = spellFlags,
        scrollPosition = scrollPosition,
        scrollableHeight = scrollableHeight,
        scripts = scripts,
        scriptOperand = scriptOperand,
        scriptComparator = scriptComparator,
        optionType = optionType,
        option = option,
        modelId = modelId,
        modelType = modelType,
        modelZoom = modelZoom,
        modelPitch = modelPitch,
        modelYaw = modelYaw,
        inventoryUsable = inventoryUsable,
        inventorySlotOffsetX = inventorySlotOffsetX,
        inventorySlotOffsetY = inventorySlotOffsetY,
        inventorySlotObjId = inventorySlotObjId,
        inventorySlotObjCount = inventorySlotObjCount,
        inventoryOptions = inventoryOptions,
        inventoryMoveReplaces = inventoryMoveReplaces,
        inventoryMarginX = inventoryMarginX,
        inventoryMarginY = inventoryMarginY,
        inventoryDraggable = inventoryDraggable,
        hoverColor = hoverColor,
        hide = hide,
        fill = fill,
        delegateHover = delegateHover,
        contentType = contentType,
        color = color,
        childX = childX,
        childY = childY,
        childId = childId,
        center = center,
        activeText = activeText,
        activeSeqId = activeSeqId,
        activeModelId = activeModelId,
        activeModelType = activeModelType,
        activeHoverColor = activeHoverColor,
        activeColor = activeColor,
        inventoryInteractAble = inventoryInteractAble,
        image = image,
        activeImage = activeImage,
        inventorySlotImage = inventorySlotImage
    )
}