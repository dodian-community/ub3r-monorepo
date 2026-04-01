package net.dodian.uber.game.content.commands

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.config.gameWorldId

internal fun isSpecialRights(client: Client): Boolean =
    client.playerGroup == 6 || client.playerGroup == 10 || client.playerGroup == 35

internal fun isBetaWorld(): Boolean = gameWorldId > 1

internal fun canUseStaffTeleport(client: Client, specialRights: Boolean): Boolean =
    client.wildyLevel <= 0 || specialRights
