package net.dodian.uber.game.libraries.commands.parsers

import net.dodian.uber.game.libraries.commands.interfaces.ICommandArgumentParser
import net.dodian.uber.game.model.entity.npc.NpcData
import net.dodian.uber.game.model.entity.player.PlayerHandler
import net.dodian.uber.game.model.item.Item
import net.dodian.uber.game.modelkt.entity.player.Player
import kotlin.reflect.KClass

class GeneralServerParsers(
    override val destination: KClass<Any> = Any::class,
    override val destinations: List<KClass<*>> = listOf(
        Player::class,
        Item::class,
        NpcData::class
    )
) : ICommandArgumentParser<Any> {

    override fun parse(input: String, type: KClass<*>?): Any? = when(type) {
        Player::class -> PlayerHandler.playersOnline.values.firstOrNull {
            it.playerName.lowercase() == input.lowercase()
        }
        else -> null
    }
}