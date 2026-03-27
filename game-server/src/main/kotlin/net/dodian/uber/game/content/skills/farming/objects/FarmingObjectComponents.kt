package net.dodian.uber.game.content.skills.farming.objects

import net.dodian.uber.game.content.skills.farming.FarmingDefinitions

object FarmingObjectComponents {
    val patchObjects: IntArray = FarmingDefinitions.patches.values()
        .flatMap { it.objectId.toList() }
        .distinct()
        .sorted()
        .toIntArray()
}
