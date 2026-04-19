package net.dodian.cache.objects

import net.dodian.uber.game.model.Position

class CacheObject(
    val def: GameObjectDef,
    val location: Position,
    val type: Int,
    val rotation: Int,
)

