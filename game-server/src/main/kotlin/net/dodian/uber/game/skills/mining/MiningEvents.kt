package net.dodian.uber.game.skills.mining

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.event.GameEvent

enum class MiningStopReason {
    USER_INTERRUPT,
    NO_PICKAXE,
    FULL_INVENTORY,
    MOVED_AWAY,
    BUSY,
    RESTED,
    DISCONNECTED,
    INVALID_ROCK,
}

data class MiningStartedEvent(
    val client: Client,
    val rock: MiningRockDef,
    val position: Position,
    val pickaxe: PickaxeDef,
) : GameEvent

data class MiningSuccessEvent(
    val client: Client,
    val rock: MiningRockDef,
    val oreItemId: Int,
    val experience: Int,
    val position: Position,
) : GameEvent

data class MiningStoppedEvent(
    val client: Client,
    val rock: MiningRockDef?,
    val position: Position?,
    val reason: MiningStopReason,
) : GameEvent
