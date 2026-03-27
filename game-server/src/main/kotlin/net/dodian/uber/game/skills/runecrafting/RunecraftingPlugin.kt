package net.dodian.uber.game.skills.runecrafting

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.skills.runecrafting.RunecraftingPouchService
import net.dodian.uber.game.skills.runecrafting.RunecraftingRequest
import net.dodian.uber.game.skills.runecrafting.RunecraftingService

object RunecraftingPlugin {
    @JvmStatic
    fun start(client: Client, request: RunecraftingRequest): Boolean = RunecraftingService.start(client, request)

    @JvmStatic
    fun fillPouch(client: Client, pouchId: Int): Boolean = RunecraftingPouchService.fill(client, pouchId)

    @JvmStatic
    fun emptyPouch(client: Client, pouchId: Int): Boolean = RunecraftingPouchService.empty(client, pouchId)

    @JvmStatic
    fun checkPouch(client: Client, pouchId: Int): Boolean = RunecraftingPouchService.check(client, pouchId)
}
