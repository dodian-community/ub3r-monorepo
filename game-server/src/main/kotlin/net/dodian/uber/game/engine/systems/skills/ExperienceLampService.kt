package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString

/** Opens the shared genie/antique lamp interface and records its selected reward mode. */
object ExperienceLampService {
    @JvmStatic fun openGenie(client: Client) = open(client, genie = true)
    @JvmStatic fun openAntique(client: Client) = open(client, genie = false)

    private fun open(client: Client, genie: Boolean) {
        if (client.inDuel || client.duelFight || client.IsBanking) {
            client.send(SendMessage("Finish what you are doing first!"))
            return
        }
        client.send(SendString("Select a skill in which you wish to gain experience!", 2810))
        client.send(SendString("", 2811))
        client.send(SendString("", 2831))
        client.genie = genie
        client.antique = !genie
        client.openInterface(2808)
    }
}
