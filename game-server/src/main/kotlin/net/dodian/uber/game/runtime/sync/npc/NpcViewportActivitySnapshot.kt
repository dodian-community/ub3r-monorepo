package net.dodian.uber.game.runtime.sync.npc

data class NpcViewportActivitySnapshot(
    val chunkActivityStamp: Long,
    val localNpcActivityStamp: Long,
)
