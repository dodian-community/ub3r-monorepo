package net.dodian.uber.plugin.protocol

import net.dodian.uber.event.impl.GameProcessEvent
import net.dodian.uber.plugin.context
import net.dodian.uber.plugin.onEvent

private val packets = context().packetMap

onEvent<GameProcessEvent.BootUp> {
    packets.eagerInitialize()
}