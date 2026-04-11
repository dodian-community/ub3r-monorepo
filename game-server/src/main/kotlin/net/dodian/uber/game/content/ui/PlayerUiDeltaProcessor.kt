package net.dodian.uber.game.content.ui

import java.util.IdentityHashMap
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.model.player.quests.QuestSend
import net.dodian.uber.game.engine.config.gameWorldId
import org.slf4j.LoggerFactory

object PlayerUiDeltaProcessor {
    private val logger = LoggerFactory.getLogger(PlayerUiDeltaProcessor::class.java)
    private val states = IdentityHashMap<Client, PlayerUiState>()
    private val globalUiPublisher = GlobalUiPublisher()

    @JvmStatic
    fun process(activePlayers: List<Client>) {
        pruneStates(activePlayers)
        val now = System.currentTimeMillis()
        val globalPayload = globalUiPublisher.publish(now)
        activePlayers.forEach { player ->
            try {
                val state = states.computeIfAbsent(player) { PlayerUiState() }
                processQuestTab(player, state, globalPayload)
                processOverlay(player, state, now, globalPayload)
                processContextMenu(player, state)
            } catch (throwable: Throwable) {
                logger.error(
                    "Player UI delta failed player={} slot={}",
                    player.playerName,
                    player.slot,
                    throwable,
                )
                player.disconnected = true
            }
        }
    }

    private fun processQuestTab(player: Client, state: PlayerUiState, payload: GlobalUiPayload) {
        if (player.questPage == 0) {
            if (state.lastQuestTabMode != 0) {
                QuestSend.questInterface(player)
                state.lastQuestTabMode = 0
            }
            return
        }

        if (state.lastQuestTabMode != 1 || state.lastGlobalUiRevisionSeen != payload.revision) {
            QuestSend.serverInterface(player, payload.uptimeText)
            state.lastQuestTabMode = 1
            state.lastGlobalUiRevisionSeen = payload.revision
        }
    }

    private fun processOverlay(player: Client, state: PlayerUiState, now: Long, payload: GlobalUiPayload) {
        val overlayText =
            if (player.mutedTill <= now) {
                if (player.invis) "You are invisible!" else ""
            } else {
                val mutedDays = ((player.mutedTill - now) / 86_400_000L).toInt()
                val mutedHours =
                    if (mutedDays == 0) {
                        (((player.mutedTill - now) / 3_600_000L).toInt()).coerceAtLeast(1)
                    } else {
                        0
                    }
                "Muted: " + if (mutedDays > 0) "$mutedDays days" else "$mutedHours hours"
            }
        player.sendCachedString(overlayText, 6572)
        state.lastOverlayText = overlayText

        val wildLevel = player.wildLevelForUi
        if (wildLevel > 0) {
            if (state.lastWildLevel != wildLevel) {
                player.setWildLevel(wildLevel)
                state.lastWildLevel = wildLevel
            }
        } else {
            // Ensure server-side wilderness state is cleared when leaving. setWildLevel(0) is
            // a no-op packet-wise, but it updates the cached wildyLevel used by teleport rules.
            if (state.lastWildLevel != 0) {
                player.setWildLevel(0)
            }
            player.sendCachedString(payload.topBarText, 6570)
            state.lastTopBarText = payload.topBarText
            if (state.lastWildLevel != 0) {
                player.sendCachedString("", 6664)
                state.lastWildLevel = 0
            }
            // Always reassert the normal walkable overlay. If another path cleared it while the
            // player was already out of wilderness, the cached lastWildLevel alone is not enough
            // to restore 6673.
            player.setWalkableInterface(6673)
        }
    }

    private fun processContextMenu(player: Client, state: PlayerUiState) {
        val primaryText = if (player.equipment[Equipment.Slot.WEAPON.id] == 4566) "Whack" else "null"
        val menuState =
            if (player.duelFight || player.inWildy()) {
                ContextMenuState(
                    slot1Text = primaryText,
                    slot1Enabled = player.equipment[Equipment.Slot.WEAPON.id] == 4566,
                    slot2Text = "null",
                    slot2Enabled = false,
                    slot3Text = "Attack",
                    slot3Enabled = true,
                    slot4Text = "Follow",
                    slot4Enabled = false,
                    slot5Text = if (player.inWildy()) "Trade with" else "null",
                    slot5Enabled = false,
                )
            } else {
                ContextMenuState(
                    slot1Text = primaryText,
                    slot1Enabled = player.equipment[Equipment.Slot.WEAPON.id] == 4566,
                    slot2Text = "Duel",
                    slot2Enabled = false,
                    slot3Text = "null",
                    slot3Enabled = true,
                    slot4Text = "Follow",
                    slot4Enabled = false,
                    slot5Text = "Trade with",
                    slot5Enabled = false,
                )
            }
        val hash = menuState.hashCode()
        if (state.lastContextMenuHash == hash) {
            return
        }
        player.setPlayerContextMenu(1, menuState.slot1Enabled, menuState.slot1Text)
        player.setPlayerContextMenu(2, menuState.slot2Enabled, menuState.slot2Text)
        player.setPlayerContextMenu(3, menuState.slot3Enabled, menuState.slot3Text)
        player.setPlayerContextMenu(4, menuState.slot4Enabled, menuState.slot4Text)
        player.setPlayerContextMenu(5, menuState.slot5Enabled, menuState.slot5Text)
        state.lastContextMenuHash = hash
    }

    private fun pruneStates(activePlayers: List<Client>) {
        val active = IdentityHashMap<Client, Boolean>(activePlayers.size)
        activePlayers.forEach { active[it] = true }
        val iterator = states.keys.iterator()
        while (iterator.hasNext()) {
            if (!active.containsKey(iterator.next())) {
                iterator.remove()
            }
        }
    }

    private data class ContextMenuState(
        val slot1Text: String,
        val slot1Enabled: Boolean,
        val slot2Text: String,
        val slot2Enabled: Boolean,
        val slot3Text: String,
        val slot3Enabled: Boolean,
        val slot4Text: String,
        val slot4Enabled: Boolean,
        val slot5Text: String,
        val slot5Enabled: Boolean,
    )
}

data class PlayerUiState(
    var lastQuestTabMode: Int = Int.MIN_VALUE,
    var lastContextMenuHash: Int = Int.MIN_VALUE,
    var lastOverlayText: String = "",
    var lastTopBarText: String = "",
    var lastWildLevel: Int = Int.MIN_VALUE,
    var lastGlobalUiRevisionSeen: Long = Long.MIN_VALUE,
)

data class GlobalUiPayload(
    val revision: Long,
    val uptimeText: String,
    val topBarText: String,
)

class GlobalUiPublisher {
    private var revision = 0L
    private var lastUptimeMinute = Long.MIN_VALUE
    private var lastPlayerCount = Int.MIN_VALUE
    private var cachedPayload =
        GlobalUiPayload(
            revision = 0L,
            uptimeText = QuestSend.getCachedUptimeText(),
            topBarText = topBarText(PlayerRegistry.getPlayerCount()),
        )

    fun publish(now: Long): GlobalUiPayload {
        val uptimeMinute = now / 60_000L
        val playerCount = PlayerRegistry.getPlayerCount()
        if (uptimeMinute != lastUptimeMinute || playerCount != lastPlayerCount) {
            revision++
            lastUptimeMinute = uptimeMinute
            lastPlayerCount = playerCount
            cachedPayload =
                GlobalUiPayload(
                    revision = revision,
                    uptimeText = QuestSend.getCachedUptimeText(),
                    topBarText = topBarText(playerCount),
                )
        }
        return cachedPayload
    }

    private fun topBarText(playerCount: Int): String {
        val serverName = if (gameWorldId == 1) "Uber Server 3.0" else "Beta World"
        return "$serverName ($playerCount online)"
    }
}

private val Client.wildLevelForUi: Int
    get() = getWildLevel()
