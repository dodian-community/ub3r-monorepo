package net.dodian.uber.game.content.combat

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry

internal fun resolveCombatTargetPlayer(slot: Int): Client? = PlayerRegistry.getClient(slot)
