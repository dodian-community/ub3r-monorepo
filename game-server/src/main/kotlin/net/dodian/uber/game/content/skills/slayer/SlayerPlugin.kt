package net.dodian.uber.game.content.skills.slayer

import java.util.ArrayList
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.slayer.SlayerService
import net.dodian.uber.game.content.skills.slayer.SlayerTaskDefinition

object SlayerPlugin {
    @JvmStatic
    fun sendCurrentTask(client: Client) = SlayerService.sendTask(client)

    @JvmStatic
    fun tasksForMaster(client: Client, masterNpcId: Int): ArrayList<SlayerTaskDefinition> =
        when (masterNpcId) {
            402 -> SlayerService.mazchnaTasks(client)
            403 -> SlayerService.vannakaTasks(client)
            405 -> SlayerService.duradelTasks(client)
            else -> arrayListOf()
        }
}
