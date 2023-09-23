package net.dodian.uber.game.modelkt.entity

enum class EntityType {
    DYNAMIC_OBJECT,
    GROUND_ITEM,
    NPC,
    PLAYER,
    PROJECTILE,
    STATIC_OBJECT
}

val EntityType.isMob get() = this == EntityType.PLAYER || this == EntityType.NPC
val EntityType.isTransient get() = this == EntityType.PROJECTILE