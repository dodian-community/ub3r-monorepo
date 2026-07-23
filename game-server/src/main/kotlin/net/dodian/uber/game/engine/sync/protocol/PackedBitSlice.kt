package net.dodian.uber.game.engine.sync.protocol

class PackedBitSlice(
    val bytes: ByteArray,
    val bitCount: Int,
) {
    init {
        require(bitCount in 0..(bytes.size * Byte.SIZE_BITS))
    }
}
