package net.dodian.uber.game.content.objects.farming

import net.dodian.uber.game.skills.farming.FarmingDefinitions

object FarmingObjectComponents {
    val patchObjects: IntArray = FarmingDefinitions.patches.values()
        .flatMap { it.objectId.toList() }
        .distinct()
        .sorted()
        .toIntArray()
}
