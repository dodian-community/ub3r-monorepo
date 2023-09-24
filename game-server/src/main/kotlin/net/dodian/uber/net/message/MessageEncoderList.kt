package net.dodian.uber.net.message

import net.dodian.uber.net.protocol.encoders.*
import net.dodian.uber.net.protocol.packets.server.*
import kotlin.reflect.KClass

class MessageEncoderList(
    private val encoders: MutableMap<KClass<out Message>, MessageEncoder<*>> = mutableMapOf(
        IdAssignmentMessage::class to IdAssignmentEncoder(),
        LogoutMessage::class to LogoutEncoder(),
        PlayerSynchronizationMessage::class to PlayerSynchronizationEncoder(),
        ServerChatMessage::class to ServerChatEncoder(),
        RegionChangeMessage::class to RegionChangeEncoder(),
        ClearRegionMessage::class to ClearRegionEncoder(),
        GroupedRegionUpdateMessage::class to GroupedRegionUpdateEncoder()
    )
) : MutableMap<KClass<out Message>, MessageEncoder<*>> by encoders