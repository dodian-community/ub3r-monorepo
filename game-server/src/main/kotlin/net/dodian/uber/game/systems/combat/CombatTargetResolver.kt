package net.dodian.uber.game.systems.combat

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client

internal fun resolveCombatTargetPlayer(slot: Int): Client? = Server.playerHandler.getClient(slot)

