package net.dodian.uber.game.runtime.sync.cache

class RootSynchronizationCache(
    val playerBlocks: SharedPlayerBlockCache = SharedPlayerBlockCache(),
    val npcBlocks: SharedNpcBlockCache = SharedNpcBlockCache(),
)
