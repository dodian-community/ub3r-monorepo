package net.dodian.uber.game.events.magic

import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

/** Fired when a player casts a spell on an NPC. */
data class MagicOnNpcEvent(
    val client: Client,
    val spellId: Int,
    val npcIndex: Int,
    val npc: Npc,
) : GameEvent

