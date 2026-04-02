package net.dodian.uber.game.content.skills.farming

import net.dodian.uber.game.content.skills.farming.FarmingData

object FarmingObjectComponents {
    val patchObjects: IntArray = FarmingData.patches.values()
        .flatMap { it.objectId.toList() }
        .distinct()
        .sorted()
        .toIntArray()
}
