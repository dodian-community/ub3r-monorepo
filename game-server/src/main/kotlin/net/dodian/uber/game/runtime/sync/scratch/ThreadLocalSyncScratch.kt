package net.dodian.uber.game.runtime.sync.scratch

import net.dodian.uber.game.netty.codec.ByteMessage

object ThreadLocalSyncScratch {
    private val scratch =
        ThreadLocal.withInitial {
            SyncScratchBuffers(
                playerUpdateBlock = ReusableByteMessage(ByteMessage.raw(8192)),
                npcUpdateBlock = ReusableByteMessage(ByteMessage.raw(16384)),
                appearanceBlock = ReusableByteMessage(ByteMessage.raw(256)),
                sharedBlock = ReusableByteMessage(ByteMessage.raw(512)),
            )
        }

    @JvmStatic
    fun playerUpdateBlock(): ByteMessage = scratch.get().playerUpdateBlock.acquire()

    @JvmStatic
    fun npcUpdateBlock(): ByteMessage = scratch.get().npcUpdateBlock.acquire()

    @JvmStatic
    fun appearanceBlock(): ByteMessage = scratch.get().appearanceBlock.acquire()

    @JvmStatic
    fun sharedBlock(): ByteMessage = scratch.get().sharedBlock.acquire()
}
