package net.dodian.uber.game.runtime.sync.scratch

data class SyncScratchBuffers(
    val playerUpdateBlock: ReusableByteMessage,
    val npcUpdateBlock: ReusableByteMessage,
    val appearanceBlock: ReusableByteMessage,
    val sharedBlock: ReusableByteMessage,
)
