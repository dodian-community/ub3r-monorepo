package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.skills.runecrafting.RunecraftingPouchService

/** Protocol bridge only; pouch gameplay rules are owned by the Runecrafting plugin. */
object RunecraftingPouchPacketService {
    @JvmStatic fun fill(client: Client, pouchId: Int): Boolean = RunecraftingPouchService.fill(client.asSkillPlayer(), pouchId)
    @JvmStatic fun empty(client: Client, pouchId: Int): Boolean = RunecraftingPouchService.empty(client.asSkillPlayer(), pouchId)
    @JvmStatic fun check(client: Client, pouchId: Int): Boolean = RunecraftingPouchService.check(client.asSkillPlayer(), pouchId)
}
