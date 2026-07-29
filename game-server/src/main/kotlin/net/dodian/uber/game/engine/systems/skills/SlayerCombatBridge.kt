package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.skills.slayer.SlayerCombatService

/** Engine adapter for Java and combat callers; Slayer rules remain in the plugin. */
object SlayerCombatBridge {
    @JvmStatic fun damageBonus(client: Client, npcId: Int, ranged: Boolean): Int = SlayerCombatService.damageBonus(client.asSkillPlayer(), npcId, ranged)
    @JvmStatic fun hasSlayerHelmet(client: Client): Boolean = SlayerCombatService.hasSlayerHelmet(client.asSkillPlayer())
}
