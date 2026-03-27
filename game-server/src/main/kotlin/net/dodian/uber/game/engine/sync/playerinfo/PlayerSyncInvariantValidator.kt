package net.dodian.uber.game.engine.sync.playerinfo

import java.util.LinkedHashSet
import net.dodian.uber.game.model.entity.player.Player

object PlayerSyncInvariantValidator {
    @JvmStatic
    fun snapshot(viewer: Player): ViewerLocalSnapshot {
        val locals = IntArray(viewer.playerListSize)
        for (index in 0 until viewer.playerListSize) {
            locals[index] = viewer.playerList[index]?.slot ?: -1
        }
        return ViewerLocalSnapshot(locals, viewer.localPlayerMembershipRevision)
    }

    @JvmStatic
    fun validateViewerLocals(viewer: Player, previousSnapshot: ViewerLocalSnapshot? = null) {
        val seen = LinkedHashSet<Player>(viewer.playerListSize)
        for (index in 0 until viewer.playerListSize) {
            val local = requireNotNull(viewer.playerList[index]) {
                "viewer ${viewer.slot} has null local entry at index=$index"
            }
            check(seen.add(local)) {
                "viewer ${viewer.slot} has duplicate local slot=${local.slot}"
            }
            check(viewer.playersUpdating.contains(local)) {
                "viewer ${viewer.slot} missing slot=${local.slot} from playersUpdating"
            }
        }
        check(viewer.playersUpdating.size == seen.size) {
            "viewer ${viewer.slot} playersUpdating size=${viewer.playersUpdating.size} locals=${seen.size}"
        }
        previousSnapshot?.let { snapshot ->
            val current = snapshot(viewer)
            if (!snapshot.localSlots.contentEquals(current.localSlots)) {
                check(current.membershipRevision > snapshot.membershipRevision) {
                    "viewer ${viewer.slot} local membership revision did not advance after local-list change"
                }
            }
        }
    }

    data class ViewerLocalSnapshot(
        val localSlots: IntArray,
        val membershipRevision: Long,
    )
}
