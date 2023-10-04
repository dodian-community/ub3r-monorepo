package net.dodian.uber.game.sync.block

import net.dodian.uber.game.modelkt.entity.setting.PrivilegeLevel
import net.dodian.uber.net.protocol.packets.client.PublicChatMessage

data class ChatBlock(
    val privilegeLevel: PrivilegeLevel,
    val chatMessage: PublicChatMessage
) : SynchronizationBlock {
    val message: String get() = chatMessage.message
    val textColor: Int get() = chatMessage.color
    val textEffects: Int get() = chatMessage.effects
    val compressedMessage: ByteArray get() = chatMessage.messageCompressed.clone()
}