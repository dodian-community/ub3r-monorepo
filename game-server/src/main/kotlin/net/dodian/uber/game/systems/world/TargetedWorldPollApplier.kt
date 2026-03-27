package net.dodian.uber.game.systems.world

import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.world.WorldPollResult

class TargetedWorldPollApplier {
    private var cachedLatestNewsId: Int? = null

    fun apply(
        result: WorldPollResult?,
        playerIndex: OnlinePlayerIndex,
    ) {
        if (result == null || result === WorldPollResult.EMPTY) {
            return
        }
        applyLatestNews(result, playerIndex)
        applyRefundNotifications(result, playerIndex)
        applyMuteAndBanState(result, playerIndex)
    }

    private fun applyLatestNews(
        result: WorldPollResult,
        playerIndex: OnlinePlayerIndex,
    ) {
        val latestNews = result.latestNewsId ?: return
        if (cachedLatestNewsId != null && latestNews <= cachedLatestNewsId!!) {
            return
        }
        cachedLatestNewsId = latestNews
        playerIndex.snapshot()
            .asSequence()
            .filter { it.loadingDone && it.latestNews != latestNews }
            .forEach { client ->
                client.latestNews = latestNews
                client.sendMessage("[SERVER]: There is a new post on the homepage! type ::news")
            }
    }

    private fun applyRefundNotifications(
        result: WorldPollResult,
        playerIndex: OnlinePlayerIndex,
    ) {
        if (result.playersWithRefunds.isEmpty()) {
            return
        }
        result.playersWithRefunds.forEach { dbId ->
            val client = playerIndex.byDbId(dbId) ?: return@forEach
            if (client.loadingDone) {
                client.sendMessage("<col=4C4B73>You have some unclaimed items to claim!")
            }
        }
    }

    private fun applyMuteAndBanState(
        result: WorldPollResult,
        playerIndex: OnlinePlayerIndex,
    ) {
        result.muteTimes.forEach { (dbId, muteTime) ->
            val client = playerIndex.byDbId(dbId) ?: return@forEach
            if (client.mutedTill != muteTime) {
                client.mutedTill = muteTime
            }
        }
        result.bannedPlayerIds.forEach { dbId ->
            playerIndex.byDbId(dbId)?.disconnected = true
        }
    }
}
