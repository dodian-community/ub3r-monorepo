package net.dodian.uber.game.model.mob.list

import net.dodian.uber.game.model.entity.player.Player

class PlayerList(
    private val players: MutableList<Player> = mutableListOf()
) : MutableList<Player> by players