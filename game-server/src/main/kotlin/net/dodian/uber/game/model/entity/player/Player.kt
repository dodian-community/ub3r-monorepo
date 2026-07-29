package net.dodian.uber.game.model.entity.player

/**
 * The server's Kotlin player entity.
 *
 * Transport is implemented by [Client]. Gameplay behavior belongs to focused services
 * and state components rather than this entity. [PlayerCore] is an internal migration
 * implementation detail which will shrink as its remaining legacy responsibilities are
 * extracted.
 */
abstract class Player(slot: Int) : PlayerCore(slot)
