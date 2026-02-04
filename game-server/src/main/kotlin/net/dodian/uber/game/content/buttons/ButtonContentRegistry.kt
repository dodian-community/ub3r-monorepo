package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object ButtonContentRegistry {
    private val logger = LoggerFactory.getLogger(ButtonContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byButtonId = ConcurrentHashMap<Int, ButtonContent>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) {
            return
        }
        register(PinHelpButtons)
        register(SettingsTabButtons)
        register(SpellbookTeleportButtons)
    }

    fun register(content: ButtonContent) {
        for (buttonId in content.buttonIds) {
            val existing = byButtonId.putIfAbsent(buttonId, content)
            if (existing != null) {
                logger.error(
                    "Duplicate ButtonContent for buttonId={} (existing={}, new={})",
                    buttonId,
                    existing::class.java.name,
                    content::class.java.name
                )
            }
        }
    }

    fun tryHandle(client: Client, buttonId: Int): Boolean {
        ensureLoaded()
        val content = byButtonId[buttonId] ?: return false
        return try {
            content.onClick(client, buttonId)
        } catch (e: Exception) {
            logger.error("Error handling buttonId={} via {}", buttonId, content::class.java.name, e)
            false
        }
    }
}
