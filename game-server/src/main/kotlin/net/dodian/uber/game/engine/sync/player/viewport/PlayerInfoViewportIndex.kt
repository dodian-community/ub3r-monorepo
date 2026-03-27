package net.dodian.uber.game.engine.sync.player.viewport

import java.util.IdentityHashMap
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.engine.sync.viewport.ViewportIndex

class PlayerInfoViewportIndex private constructor(
    private val slotsByViewer: IdentityHashMap<Player, IntArray>,
    private val signatureByViewer: IdentityHashMap<Player, Int>,
) {
    fun visibleSlots(viewer: Player): IntArray = slotsByViewer[viewer] ?: IntArray(0)

    fun visibleSignature(viewer: Player): Int = signatureByViewer[viewer] ?: 0

    companion object {
        fun build(viewers: List<Client>, viewportIndex: ViewportIndex?): PlayerInfoViewportIndex {
            val slotsByViewer = IdentityHashMap<Player, IntArray>(viewers.size)
            val signatureByViewer = IdentityHashMap<Player, Int>(viewers.size)
            viewers.forEach { viewer ->
                // Viewport snapshots are an optimization only. If they're unavailable (or temporarily
                // empty during lifecycle transitions), fall back to the active player list so that
                // player visibility never collapses to "no locals" (which makes players invisible).
                val snapshotPlayers = viewportIndex?.snapshotFor(viewer)?.players
                val candidates =
                    if (snapshotPlayers.isNullOrEmpty()) {
                        viewers
                    } else {
                        snapshotPlayers
                    }
                val visible = ArrayList<Int>(candidates.size)
                var signature = 1
                for (other in candidates) {
                    if (viewer === other || !other.isActive) {
                        continue
                    }
                    if (!viewer.withinDistance(other)) {
                        continue
                    }
                    if (other.invis && !viewer.invis) {
                        continue
                    }
                    visible += other.slot
                    signature = 31 * signature + other.slot
                }
                slotsByViewer[viewer] = visible.toIntArray()
                signatureByViewer[viewer] = signature
            }
            return PlayerInfoViewportIndex(slotsByViewer, signatureByViewer)
        }
    }
}
