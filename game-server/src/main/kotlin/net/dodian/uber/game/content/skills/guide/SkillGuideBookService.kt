package net.dodian.uber.game.content.skills.guide

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.systems.skills.sendFilterMessage

object SkillGuideBookService {
    @JvmStatic
    fun open(client: Client) {
        client.sendFilterMessage("this is me a guide book!")
        client.clearQuestInterface()
        client.openInterface(8134)
        client.sendString("Newcomer's Guide", 8144)
        client.sendString("---------------------------", 8145)
        client.sendString("Welcome to Dodian.net!", 8147)
        client.sendString("This guide is to help new players to get a general", 8148)
        client.sendString("understanding of how Dodian works!", 8149)
        client.sendString("", 8150)
        client.sendString("For specific boss or skill locations", 8151)
        client.sendString("navigate to the 'Guides' section of the forums.", 8152)
        client.sendString("", 8153)
        client.sendString("Here in Yanille, there are various enemies to kill,", 8154)
        client.sendString("with armor rewards that get better the higher their level.", 8155)
        client.sendString("", 8156)
        client.sendString("From Yanille, you can also head North-East to access", 8157)
        client.sendString("the mining area or South-West", 8158)
        client.sendString("up the stairs in the magic guild to access the essence mine.", 8159)
        client.sendString("", 8160)
        client.sendString("If you navigate over to your spellbook, you will see", 8161)
        client.sendString("some teleports, these all lead to key points on the server", 8162)
        client.sendString("", 8163)
        client.sendString("Seers, Catherby, Fishing Guild, and Gnome Stronghold", 8164)
        client.sendString("teleports will all bring you to skilling locations.", 8165)
        client.sendString("", 8166)
        client.sendString("Legends Guild, and Taverly teleports", 8167)
        client.sendString("will all bring you to locations with more monsters to train on.", 8168)
        client.sendString("", 8169)
        client.sendString("Teleporting to Taverly and heading up the path", 8170)
        client.sendString("is how you access the Slayer Master!", 8171)
        client.sendString("", 8172)
        client.sendString("If you have more questions please visit the 'Guides'", 8173)
        client.sendString("section of the forums, and if you still can't find the answer.", 8174)
        client.sendString("Feel free to just ask a moderator!", 8175)
        client.sendString("---------------------------", 8176)
    }
}
