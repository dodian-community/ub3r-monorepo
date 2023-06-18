package net.dodian.utilities

import net.dodian.config.groupsConfig
import net.dodian.extensions.containsAny
import net.dodian.uber.game.model.entity.player.Client

enum class RightsFlag {
    Banned,
    Muted,
    TradeLocked,

    Premium,
    BetaTester,
    RetiredStaff,

    TrialModerator,
    Moderator,
    Developer,
    Administrator,
    HiddenAdministrator
}

fun populatePlayerFlags(player: Client) {
    val groups = player.playerMemberGroups + player.playerGroup
    player.rightsFlags = arrayListOf<RightsFlag>()

    groups.forEach {
        if (groupsConfig.banned.contains(it) && !player.rightsFlags.contains(RightsFlag.Banned))
            player.rightsFlags += RightsFlag.Banned

        if (groupsConfig.muted.contains(it) && !player.rightsFlags.contains(RightsFlag.Muted))
            player.rightsFlags += RightsFlag.Muted

        if (groupsConfig.tradeLocked.contains(it) && !player.rightsFlags.contains(RightsFlag.TradeLocked))
            player.rightsFlags += RightsFlag.TradeLocked

        if (groupsConfig.premium.contains(it) && !player.rightsFlags.contains(RightsFlag.Premium))
            player.rightsFlags += RightsFlag.Premium

        if (groupsConfig.betaTester.contains(it) && !player.rightsFlags.contains(RightsFlag.BetaTester))
            player.rightsFlags += RightsFlag.BetaTester

        if (groupsConfig.retiredStaff.contains(it) && !player.rightsFlags.contains(RightsFlag.RetiredStaff))
            player.rightsFlags += RightsFlag.RetiredStaff

        if (groupsConfig.trialModerator.contains(it) && !player.rightsFlags.contains(RightsFlag.TrialModerator))
            player.rightsFlags += RightsFlag.TrialModerator

        if (groupsConfig.moderator.contains(it) && !player.rightsFlags.contains(RightsFlag.Moderator))
            player.rightsFlags += RightsFlag.Moderator

        if (groupsConfig.developer.contains(it) && !player.rightsFlags.contains(RightsFlag.Developer))
            player.rightsFlags += RightsFlag.Developer

        if (groupsConfig.administrator.contains(it) && !player.rightsFlags.contains(RightsFlag.Administrator))
            player.rightsFlags += RightsFlag.Administrator

        if (groupsConfig.hiddenAdministrator.contains(it) && !player.rightsFlags.contains(RightsFlag.HiddenAdministrator))
            player.rightsFlags += RightsFlag.HiddenAdministrator
    }

    if (player.rightsFlags.containsAny(RightsFlag.TrialModerator, RightsFlag.Moderator))
        player.playerRights = 1

    if (player.rightsFlags.containsAny(RightsFlag.Administrator, RightsFlag.Developer))
        player.playerRights = 2

    if (player.rightsFlags.containsAny(RightsFlag.HiddenAdministrator))
        player.playerRights = 3
}