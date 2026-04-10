package net.dodian.uber.game.engine.sync.cache

class RootSynchronizationCache(
    val playerBlocks: SharedPlayerBlockCache = SharedPlayerBlockCache(),
    val npcBlocks: SharedNpcBlockCache = SharedNpcBlockCache(),
)
