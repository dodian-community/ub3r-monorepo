package net.dodian.uber.game.content.dialogue.legacy.core

import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory

object DialogueDebug {
    private val logger = LoggerFactory.getLogger(DialogueDebug::class.java)
    private val enabled: Boolean = java.lang.Boolean.getBoolean("dodian.dialogue.debug")

    @JvmStatic
    fun warnUnexpectedNpc(module: String, client: Client, expectedNpcIds: Set<Int>) {
        if (!enabled || expectedNpcIds.isEmpty()) {
            return
        }
        if (client.NpcTalkTo !in expectedNpcIds) {
            logger.warn(
                "Dialogue module {} rendered dialogueId={} with unexpected NpcTalkTo={} (expected={})",
                module,
                client.NpcDialogue,
                client.NpcTalkTo,
                expectedNpcIds
            )
        }
    }
}
