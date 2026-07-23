package net.dodian.uber.game.engine.sync.protocol

/**
 * Immutable, plane-aware update block prepared during synchronization PREP.
 *
 * [fixedBytes] is MSB-first and only [fixedBitCount] bits are significant. Variable bytes stay
 * byte-aligned and are appended after the packet-wide fixed plane.
 */
class PackedUpdateBlock(
    val mask: Int,
    val fixedBytes: ByteArray,
    val fixedBitCount: Int,
    val variableBytes: ByteArray,
) {
    init {
        require(mask != 0) { "Packed update blocks require a non-zero mask." }
        require(fixedBitCount in 0..(fixedBytes.size * Byte.SIZE_BITS)) {
            "Invalid fixed bit count: $fixedBitCount"
        }
    }

    val maskBytesPlayer: Int = if (mask >= 0x100) 2 else 1
    val maskBytesNpc: Int = 1
    val fixedByteCount: Int = (fixedBitCount + 7) ushr 3
    val totalWireBytesPlayer: Int = maskBytesPlayer + fixedByteCount + variableBytes.size
    val totalWireBytesNpc: Int = maskBytesNpc + fixedByteCount + variableBytes.size

    fun estimatedWireBytes(extendedPlayerMask: Boolean): Int =
        if (extendedPlayerMask) totalWireBytesPlayer else totalWireBytesNpc
}
