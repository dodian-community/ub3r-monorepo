package net.dodian.uber.game.runtime.sync.player.root

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.runtime.sync.player.viewport.PlayerInfoViewportIndex

data class RootPlayerInfoCycle(
    val viewers: List<Client>,
    val viewportIndex: PlayerInfoViewportIndex,
    val subjectStates: Map<Int, SubjectPlayerInfoState>,
)
