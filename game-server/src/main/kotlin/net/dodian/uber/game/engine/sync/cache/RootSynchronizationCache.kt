package net.dodian.uber.game.engine.sync.cache

class RootSynchronizationCache(
    val playerBlocks: SharedPlayerBlockCache = SharedPlayerBlockCache(),
    val npcBlocks: SharedNpcBlockCache = SharedNpcBlockCache(),
    val playerMovements: SharedPlayerMovementCache = SharedPlayerMovementCache(),
    val npcMovements: SharedNpcMovementCache = SharedNpcMovementCache(),
)
