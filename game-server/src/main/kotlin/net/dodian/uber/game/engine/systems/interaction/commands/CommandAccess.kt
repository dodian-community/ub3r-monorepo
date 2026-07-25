package net.dodian.uber.game.engine.systems.interaction.commands

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.config.gameWorldId

internal fun isSpecialRights(client: Client): Boolean =
    net.dodian.uber.game.engine.config.rankAdminGroupIds.contains(client.playerGroup)

internal fun isBetaWorld(): Boolean = gameWorldId > 1

internal fun canUseStaffTeleport(client: Client, specialRights: Boolean): Boolean =
    client.wildyLevel <= 0 || specialRights