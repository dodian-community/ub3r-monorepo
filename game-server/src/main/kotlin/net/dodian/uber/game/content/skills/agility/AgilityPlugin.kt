package net.dodian.uber.game.content.skills.agility

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.skills.agility.AgilityCourseService
import net.dodian.uber.game.content.skills.agility.DesertCarpetService
import net.dodian.uber.game.content.skills.agility.WerewolfCourseService

object AgilityPlugin {
    @JvmStatic
    fun course(client: Client): AgilityCourseService = AgilityCourseService(client)

    @JvmStatic
    fun werewolf(client: Client): WerewolfCourseService = WerewolfCourseService(client)

    @JvmStatic
    fun desertCarpet(client: Client): DesertCarpetService = DesertCarpetService(client)
}
