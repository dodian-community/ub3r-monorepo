package net.dodian.uber.net.message

import net.dodian.uber.net.protocol.encoders.*
import net.dodian.uber.net.protocol.encoders.region.*
import net.dodian.uber.net.protocol.packets.server.*
import net.dodian.uber.net.protocol.packets.server.region.*
import kotlin.reflect.KClass

class MessageEncoderList(
    private val encoders: MutableMap<KClass<out Message>, MessageEncoder<*>> = mutableMapOf(
        IdAssignmentMessage::class to IdAssignmentMessageEncoder(),
        LogoutMessage::class to LogoutEncoder(),
        PlayerSynchronizationMessage::class to PlayerSynchronizationEncoder(),
        ServerChatMessage::class to ServerChatEncoder(),
        RegionChangeMessage::class to RegionChangeMessageEncoder(),
        ClearRegionMessage::class to ClearRegionMessageEncoder(),
        GroupedRegionUpdateMessage::class to GroupedRegionUpdateMessageEncoder(),
        SwitchTabInterfaceMessage::class to SwitchTabInterfaceMessageEncoder(),
        RemoveObjectMessage::class to RemoveObjectMessageEncoder(),
        RemoveTileItemMessage::class to RemoveTileItemMessageEncoder(),
        SendObjectMessage::class to SendObjectMessageEncoder(),
        SendPublicTileItemMessage::class to SendPublicTileItemMessageEncoder(),
        SendTileItemMessage::class to SendTileItemMessageEncoder(),
        UpdateTileItemMessage::class to UpdateTileItemMessageEncoder(),
        SetWidgetTextMessage::class to SetWidgetTextMessageEncoder(),
        OpenOverlayMessage::class to OpenOverlayMessageEncoder(),
        ConfigMessage::class to ConfigMessageEncoder()
    )
) : MutableMap<KClass<out Message>, MessageEncoder<*>> by encoders