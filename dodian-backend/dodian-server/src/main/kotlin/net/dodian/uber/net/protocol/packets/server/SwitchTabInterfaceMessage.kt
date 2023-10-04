package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

data class SwitchTabInterfaceMessage(
    val tab: Int,
    val interfaceId: Int
) : Message()