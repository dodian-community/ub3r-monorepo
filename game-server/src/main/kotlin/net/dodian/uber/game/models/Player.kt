package net.dodian.uber.game.models

import net.dodian.uber.game.model.Position
import net.dodian.utilities.RightsFlag

data class Player(
    val username: String,
    val rightsFlags: List<RightsFlag>,
    val rightsLevel: Int,
    val position: Position
)