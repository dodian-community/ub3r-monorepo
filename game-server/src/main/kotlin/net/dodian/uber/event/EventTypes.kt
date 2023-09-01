package net.dodian.uber.event

import net.dodian.uber.game.model.entity.player.Player

typealias TypeGameEvent = Event<Unit>

typealias TypePlayerEvent = Event<Player>
typealias TypePlayerKeyedEvent = KeyedEvent<Player>
