package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.utilities.Misc

object SpellbookTeleportButtons : ButtonContent {
    private object Buttons {
        const val YANILLE_HOME = 21741
        const val SEERS = 13035
        const val ARDOUGNE = 13045
        const val CATHERBY = 13053
        const val LEGENDS_GUILD = 13061
        const val TAVERLY = 13069
        const val FISHING_GUILD = 13079
        const val GNOME_VILLAGE = 13087
        const val EDGEVILLE = 13095
    }

    private data class Teleport(
        val buttonIds: IntArray,
        val baseX: Int,
        val randX: Int,
        val baseY: Int,
        val randY: Int,
        val height: Int = 0,
        val premium: Boolean = false,
    )

    private val teleports: List<Teleport> = listOf(
        Teleport(intArrayOf(Buttons.YANILLE_HOME), baseX = 2604, randX = 6, baseY = 3101, randY = 3),
        Teleport(intArrayOf(Buttons.SEERS), baseX = 2722, randX = 6, baseY = 3484, randY = 2),
        Teleport(intArrayOf(Buttons.ARDOUGNE), baseX = 2660, randX = 4, baseY = 3306, randY = 4),
        Teleport(intArrayOf(Buttons.CATHERBY), baseX = 2802, randX = 4, baseY = 3432, randY = 3),
        Teleport(intArrayOf(Buttons.LEGENDS_GUILD), baseX = 2726, randX = 5, baseY = 3346, randY = 2),
        Teleport(intArrayOf(Buttons.TAVERLY), baseX = 2893, randX = 4, baseY = 3454, randY = 3),
        Teleport(intArrayOf(Buttons.FISHING_GUILD), baseX = 2596, randX = 3, baseY = 3406, randY = 4, premium = true),
        Teleport(intArrayOf(Buttons.GNOME_VILLAGE), baseX = 2472, randX = 6, baseY = 3436, randY = 3),
        Teleport(intArrayOf(Buttons.EDGEVILLE), baseX = 3085, randX = 4, baseY = 3488, randY = 4),
    )

    private val byButtonId: Map<Int, Teleport> = teleports
        .flatMap { teleport -> teleport.buttonIds.map { buttonId -> buttonId to teleport } }
        .toMap()

    override val buttonIds: IntArray = teleports
        .flatMap { it.buttonIds.asList() }
        .toIntArray()

    override fun onClick(client: Client, buttonId: Int): Boolean {
        val teleport = byButtonId[buttonId] ?: return false
        client.triggerTele(
            teleport.baseX + Misc.random(teleport.randX),
            teleport.baseY + Misc.random(teleport.randY),
            teleport.height,
            teleport.premium
        )
        return true
    }
}
