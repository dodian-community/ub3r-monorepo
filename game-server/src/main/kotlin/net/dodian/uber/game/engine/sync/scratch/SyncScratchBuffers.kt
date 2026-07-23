package net.dodian.uber.game.engine.sync.scratch

data class SyncScratchBuffers(
    val appearanceBlock: ReusableByteMessage,
    val packedFixedBlock: ReusableByteMessage,
    val packedVariableBlock: ReusableByteMessage,
)
