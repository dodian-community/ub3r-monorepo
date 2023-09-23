package net.dodian.uber.net.message

import net.dodian.uber.net.protocol.encoders.IdAssignmentEncoder
import net.dodian.uber.net.protocol.encoders.LogoutEncoder
import net.dodian.uber.net.protocol.encoders.PlayerSynchronizationEncoder
import net.dodian.uber.net.protocol.encoders.ServerChatEncoder
import net.dodian.uber.net.protocol.packets.server.IdAssignment
import net.dodian.uber.net.protocol.packets.server.Logout
import net.dodian.uber.net.protocol.packets.server.PlayerSynchronization
import net.dodian.uber.net.protocol.packets.server.ServerChat
import kotlin.reflect.KClass

class MessageEncoderList(
    private val encoders: MutableMap<KClass<out Message>, MessageEncoder<*>> = mutableMapOf(
        IdAssignment::class to IdAssignmentEncoder(),
        Logout::class to LogoutEncoder(),
        PlayerSynchronization::class to PlayerSynchronizationEncoder(),
        ServerChat::class to ServerChatEncoder()
    )
) : MutableMap<KClass<out Message>, MessageEncoder<*>> by encoders