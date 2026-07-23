package net.dodian.uber.game.engine.sync.scratch

import net.dodian.uber.game.netty.codec.ByteMessage

object ThreadLocalSyncScratch {
    private val scratch =
        ThreadLocal.withInitial {
            SyncScratchBuffers(
                appearanceBlock = ReusableByteMessage(ByteMessage.raw(256)),
                packedFixedBlock = ReusableByteMessage(ByteMessage.raw(64)),
                packedVariableBlock = ReusableByteMessage(ByteMessage.raw(512)),
            )
        }

    @JvmStatic
    fun appearanceBlock(): ByteMessage = scratch.get().appearanceBlock.acquire()

    @JvmStatic
    fun packedFixedBlock(): ByteMessage = scratch.get().packedFixedBlock.acquire()

    @JvmStatic
    fun packedVariableBlock(): ByteMessage = scratch.get().packedVariableBlock.acquire()
}
