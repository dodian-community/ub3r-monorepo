package net.dodian.uber.net.protocol.packets.server

import net.dodian.uber.net.message.Message

data class IdAssignmentMessage(
    val id: Int,
    val isPremium: Boolean
) : Message()