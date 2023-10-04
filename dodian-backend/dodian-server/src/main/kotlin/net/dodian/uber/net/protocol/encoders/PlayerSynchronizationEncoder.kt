package net.dodian.uber.net.protocol.encoders

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.game.modelkt.Item
import net.dodian.uber.game.modelkt.entity.player.*
import net.dodian.uber.game.sync.block.AnimationBlock
import net.dodian.uber.game.sync.block.AppearanceBlock
import net.dodian.uber.game.sync.block.ChatBlock
import net.dodian.uber.game.sync.segment.MovementSegment
import net.dodian.uber.game.sync.segment.SegmentType
import net.dodian.uber.game.sync.segment.SynchronizationSegment
import net.dodian.uber.game.sync.segment.TeleportSegment
import net.dodian.uber.net.codec.game.*
import net.dodian.uber.net.message.MessageEncoder
import net.dodian.uber.net.message.meta.PacketType
import net.dodian.uber.net.protocol.packets.server.PlayerSynchronizationMessage

private val logger = InlineLogger()

class PlayerSynchronizationEncoder : MessageEncoder<PlayerSynchronizationMessage>() {

    override fun encode(message: PlayerSynchronizationMessage): GamePacket {
        val builder = GamePacketBuilder(81, PacketType.VARIABLE_SHORT)
        builder.switchToBitAccess()

        val blockBuilder = GamePacketBuilder()

        builder.putMovementUpdate(message.segment, message)
        blockBuilder.putBlocks(message.segment)

        builder.putBits(8, message.localPlayers)

        message.segments.forEach { segment ->
            when (segment.segmentType) {
                SegmentType.REMOVE_MOB -> {} // TODO:
                SegmentType.ADD_MOB -> {} // TODO:
                else -> {
                    builder.putMovementUpdate(segment, message)
                    blockBuilder.putBlocks(segment)
                }
            }
        }

        if (blockBuilder.length > 0) {
            builder.putBits(11, 2047)
            builder.switchToByteAccess()
            builder.putRawBuilder(blockBuilder)
        } else {
            builder.switchToByteAccess()
        }

        return builder.toGamePacket()
    }

    private fun GamePacketBuilder.putMovementUpdate(
        segment: SynchronizationSegment,
        message: PlayerSynchronizationMessage
    ) {
        val updateRequired = segment.blockSet.size > 0
        when (segment.segmentType) {
            SegmentType.TELEPORT -> {
                val destination = (segment as TeleportSegment).destination
                putBits(1, 1)
                putBits(2, 3)
                putBits(2, destination.height)
                putBits(1, if (message.hasRegionChanged) 0 else 1)
                putBits(1, if (updateRequired) 1 else 0)
                putBits(7, destination.localY(message.lastKnownRegion))
                putBits(7, destination.localX(message.lastKnownRegion))
            }

            SegmentType.RUN -> {
                val directions = (segment as MovementSegment).directions
                putBits(1, 1)
                putBits(2, 2)
                putBits(3, directions[0].toInt())
                putBits(3, directions[1].toInt())
                putBits(1, if (updateRequired) 1 else 0)
            }

            SegmentType.WALK -> {
                val directions = (segment as MovementSegment).directions
                putBits(1, 1)
                putBits(2, 1)
                putBits(3, directions[0].toInt())
                putBits(1, if (updateRequired) 1 else 0)
            }

            else -> {
                when (updateRequired) {
                    true -> {
                        putBits(1, 1)
                        putBits(2, 0)
                    }

                    false -> putBits(1, 0)
                }
            }
        }
    }

    private fun GamePacketBuilder.putBlocks(segment: SynchronizationSegment) {
        val blockSet = segment.blockSet
        if (blockSet.size <= 0)
            return

        var mask = 0

        if (blockSet.contains(AnimationBlock::class))
            mask = mask or 0x8

        if (blockSet.contains(ChatBlock::class))
            mask = mask or 0x80

        if (blockSet.contains(AppearanceBlock::class))
            mask = mask or 0x10

        if (mask >= 0x100) {
            mask = mask or 0x40
            put(DataType.SHORT, DataOrder.LITTLE, mask)
        } else {
            put(DataType.BYTE, mask)
        }

        if (blockSet.contains(AnimationBlock::class))
            putAnimationBlock(blockSet[AnimationBlock::class] ?: error("No animation block..."))

        if (blockSet.contains(ChatBlock::class))
            putChatBlock(blockSet[ChatBlock::class] ?: error("No animation block..."))

        if (blockSet.contains(AppearanceBlock::class))
            putAppearanceBlock(blockSet[AppearanceBlock::class] ?: error("No appearance block..."))
    }

    private fun GamePacketBuilder.putAnimationBlock(block: AnimationBlock) {
        val animation = block.animation
        put(DataType.SHORT, DataOrder.LITTLE, animation.id)
        put(DataType.BYTE, DataTransformation.NEGATE, animation.delay)
    }

    private fun GamePacketBuilder.putChatBlock(block: ChatBlock) {
        val bytes = block.compressedMessage
        put(DataType.SHORT, DataOrder.LITTLE, block.textColor shl 8 or block.textEffects)
        put(DataType.BYTE, block.privilegeLevel.ordinal)
        put(DataType.BYTE, DataTransformation.NEGATE, bytes.size)
        putBytesReverse(bytes)
    }

    private fun GamePacketBuilder.putAppearanceBlock(block: AppearanceBlock) {
        val appearance = block.appearance
        val playerProperties = GamePacketBuilder()

        playerProperties.put(DataType.BYTE, appearance.gender.ordinal)
        playerProperties.put(DataType.BYTE, 0)

        if (block.isAppearingAsNpc) {
            playerProperties.put(DataType.BYTE, 255)
            playerProperties.put(DataType.BYTE, 255)
            playerProperties.put(DataType.SHORT, block.npcId)
        } else {
            val equipment = block.equipment
            val style = appearance.style

            var item: Item?
            var chest: Item?
            var helm: Item?

            for (slot in 0 until 4) {
                if (equipment[slot].also { item = it } != null) {
                    playerProperties.put(DataType.SHORT, 0x200 + item!!.id)
                } else {
                    playerProperties.put(DataType.BYTE, 0)
                }
            }

            if (equipment[CHEST].also { chest = it } != null) {
                playerProperties.put(DataType.SHORT, 0x200 + chest!!.id)
            } else {
                playerProperties.put(DataType.SHORT, 0x100 + style[2])
            }

            if (equipment[SHIELD].also { item = it } != null) {
                playerProperties.put(DataType.SHORT, 0x200 + item!!.id)
            } else {
                playerProperties.put(DataType.BYTE, 0)
            }

            if (chest != null) {
                // TODO:
                playerProperties.put(DataType.SHORT, 0x100 + style[3])
            } else {
                playerProperties.put(DataType.SHORT, 0x100 + style[3])
            }

            if (equipment[LEGS].also { item = it } != null) {
                playerProperties.put(DataType.SHORT, 0x200 + item!!.id)
            } else {
                playerProperties.put(DataType.SHORT, 0x100 + style[5])
            }

            if (equipment[HAT].also { helm = it } != null) {
                // TODO:
                playerProperties.put(DataType.SHORT, 0x100 + style[0])
            } else {
                playerProperties.put(DataType.SHORT, 0x100 + style[0])
            }

            if (equipment[HANDS].also { item = it } != null) {
                playerProperties.put(DataType.SHORT, 0x200 + item!!.id)
            } else {
                playerProperties.put(DataType.SHORT, 0x100 + style[4])
            }

            if (equipment[FEET].also { item = it } != null) {
                playerProperties.put(DataType.SHORT, 0x200 + item!!.id)
            } else {
                playerProperties.put(DataType.SHORT, 0x100 + style[6])
            }

            // TODO:
            playerProperties.put(DataType.SHORT, 0x100 + style[1])
        }

        val colors = appearance.colors
        colors.forEach { playerProperties.put(DataType.BYTE, it) }

        playerProperties.put(DataType.SHORT, 0x328)
        playerProperties.put(DataType.SHORT, 0x337)
        playerProperties.put(DataType.SHORT, 0x333)
        playerProperties.put(DataType.SHORT, 0x334)
        playerProperties.put(DataType.SHORT, 0x335)
        playerProperties.put(DataType.SHORT, 0x336)
        playerProperties.put(DataType.SHORT, 0x338)

        playerProperties.put(DataType.LONG, block.name)
        playerProperties.put(DataType.BYTE, block.combat)
        playerProperties.put(DataType.SHORT, block.skill)

        put(DataType.BYTE, DataTransformation.NEGATE, playerProperties.length)
        putRawBuilder(playerProperties)
    }
}