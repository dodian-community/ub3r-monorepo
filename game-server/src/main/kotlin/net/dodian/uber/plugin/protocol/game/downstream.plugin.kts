package net.dodian.uber.plugin.protocol.game

import com.github.michaelbull.logging.InlineLogger
import io.netty.buffer.ByteBuf
import net.dodian.uber.game.sync.block.ForceMovementBlock
import net.dodian.uber.game.sync.segment.SegmentType
import net.dodian.uber.game.sync.segment.SynchronizationSegment
import net.dodian.uber.plugin.context
import net.dodian.uber.protocol.downstream.*
import org.openrs2.buffer.BitBuf
import org.openrs2.buffer.writeByteA
import org.openrs2.buffer.writeShortA

private val logger = InlineLogger()

val packets = context().packetMap.downstream

/*packets.register<SidebarOpen> {
    opcode = 106
    encode { packet, buf ->
        // TODO: Not sure if this is the right buffer writer
        buf.writeByte(packet.sidebarId)
    }
}

packets.register<RegionUpdate> {
    opcode = 85
    encode { packet, buf ->
        // TODO: Not sure if this is the right buffer writer
        buf.writeByte(packet.region.getLocalY(packet.player))
        buf.writeByte(packet.region.getLocalX(packet.player))
    }
}

packets.register<Logout> {
    opcode = 109
    encode { _, _ -> }
}

packets.register<InterfaceClose> {
    //opcode = 219
    //encode { _, _ -> }
}
*/

packets.register<RegionClear> {
    opcode = 64
    encode { packet, buf ->
        buf.writeByte(-packet.region.getLocalX(packet.player))
        buf.writeByte(128 - packet.region.getLocalY(packet.player))
    }
}

packets.register<SetRegionUpdate> {
    opcode = 85
    encode { packet, buf ->
        buf.writeByte(-packet.region.getLocalY(packet.player))
        buf.writeByte(-packet.region.getLocalX(packet.player))
    }
}

packets.register<RegionChange> {
    opcode = 73
    encode { packet, buf ->
        buf.writeShortA(packet.position.regionX)
        buf.writeShort(packet.position.regionY)
    }
}

packets.register<PlayerSynchronization> {
    opcode = 81
    length = variableShortLength
    encode { packet, buf ->
        val bitBuf = BitBuf(buf)

        bitBuf.writeBits(1, 1)
        bitBuf.writeBits(2, 3)
        bitBuf.writeBits(2, 0)
        bitBuf.writeBits(1, 1)
        bitBuf.writeBits(1, 1)
        bitBuf.writeBits(7, 48)
        bitBuf.writeBits(7, 48)
        bitBuf.writeBits(8, 0)
        bitBuf.writeBits(11, 2047)

        buf.writeByte(0)

        //bitBuf.putMovementUpdate(packet.segment)
        //buf.putBlocks(packet.segment)

        //bitBuf.writeBits(8, packet.localPlayers)

        //for (segment in packet.segments) {
        //    when (segment.type) {
        //        SegmentType.REMOVE_MOB -> {
        //            // TODO: remove player update
        //        }

        //        SegmentType.ADD_MOB -> {
        //            // TODO: add player update
        //            buf.putBlocks(segment)
        //        }

        //        else -> {
        //            bitBuf.putMovementUpdate(packet.segment)
        //            buf.putBlocks(packet.segment)
        //        }
        //    }
        //}

        //if (bitBuf.writerIndex() > 0) {
        //    bitBuf.writeBits(11, 2047)
        //}
    }
}

fun ByteBuf.putBlocks(segment: SynchronizationSegment) {
    val blockSet = segment.blockSet
    if (blockSet.size <= 0) return

    var mask = 0

    if (blockSet.contains(ForceMovementBlock::class.java)) {
        mask = mask or 0x400
    }

    // TODO: GraphicsBlock
    // TODO: AnimationBlock
    // TODO: ForceChatBlock
    // TODO: ChatBlock
    // TODO: InteractingMobBlock
    // TODO: AppearanceBlock
    // TODO: TurnToPositionBlock
    // TODO: HitUpdateBlock
    // TODO: SecondaryHitUpdateBlock

    if (mask >= 0x100) {
        mask = mask or 0x40
        writeShortLE(mask)
    } else {
        writeByte(mask)
    }
}

fun BitBuf.putMovementUpdate(segment: SynchronizationSegment) {
    val updateRequired = segment.blockSet.size > 0

    when (segment.type) {
        SegmentType.TELEPORT -> {
            // TODO: Add teleport segment
        }

        SegmentType.RUN -> {
            // TODO: Add run segment
        }

        SegmentType.WALK -> {
            // TODO: Add walk segment
        }

        else -> {
            if (updateRequired) {
                this.writeBits(1, 1)
                this.writeBits(2, 0)
            } else {
                this.writeBits(1, 0)
            }
        }
    }
}