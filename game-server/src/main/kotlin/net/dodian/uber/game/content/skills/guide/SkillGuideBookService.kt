package net.dodian.uber.game.content.skills.guide

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.content.skills.core.runtime.sendFilterMessage

object SkillGuideBookService {
    @JvmStatic
    fun open(client: Client) {
        client.sendFilterMessage("this is me a guide book!")
        client.clearQuestInterface()
        client.showInterface(8134)
        client.send(SendString("Newcomer's Guide", 8144))
        client.send(SendString("---------------------------", 8145))
        client.send(SendString("Welcome to Dodian.net!", 8147))
        client.send(SendString("This guide is to help new players to get a general", 8148))
        client.send(SendString("understanding of how Dodian works!", 8149))
        client.send(SendString("", 8150))
        client.send(SendString("For specific boss or skill locations", 8151))
        client.send(SendString("navigate to the 'Guides' section of the forums.", 8152))
        client.send(SendString("", 8153))
        client.send(SendString("Here in Yanille, there are various enemies to kill,", 8154))
        client.send(SendString("with armor rewards that get better the higher their level.", 8155))
        client.send(SendString("", 8156))
        client.send(SendString("From Yanille, you can also head North-East to access", 8157))
        client.send(SendString("the mining area or South-West", 8158))
        client.send(SendString("up the stairs in the magic guild to access the essence mine.", 8159))
        client.send(SendString("", 8160))
        client.send(SendString("If you navigate over to your spellbook, you will see", 8161))
        client.send(SendString("some teleports, these all lead to key points on the server", 8162))
        client.send(SendString("", 8163))
        client.send(SendString("Seers, Catherby, Fishing Guild, and Gnome Stronghold", 8164))
        client.send(SendString("teleports will all bring you to skilling locations.", 8165))
        client.send(SendString("", 8166))
        client.send(SendString("Legends Guild, and Taverly teleports", 8167))
        client.send(SendString("will all bring you to locations with more monsters to train on.", 8168))
        client.send(SendString("", 8169))
        client.send(SendString("Teleporting to Taverly and heading up the path", 8170))
        client.send(SendString("is how you access the Slayer Master!", 8171))
        client.send(SendString("", 8172))
        client.send(SendString("If you have more questions please visit the 'Guides'", 8173))
        client.send(SendString("section of the forums, and if you still can't find the answer.", 8174))
        client.send(SendString("Feel free to just ask a moderator!", 8175))
        client.send(SendString("---------------------------", 8176))
    }
}
