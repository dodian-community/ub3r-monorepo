package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.content.buttons.teleports.*
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
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
        register(net.dodian.uber.game.content.buttons.dialogue.NpcDialoguePromptButtons)
        register(net.dodian.uber.game.content.buttons.banking.BankDepositButtons)
        register(net.dodian.uber.game.content.buttons.banking.BankInterfaceButtons)
        register(net.dodian.uber.game.content.buttons.crafting.MakeAmountButtons)
        register(net.dodian.uber.game.content.buttons.crafting.SmeltingButtons)
        register(net.dodian.uber.game.content.buttons.crafting.LeatherCraftButtons)
        register(net.dodian.uber.game.content.buttons.crafting.TanningButtons)
        register(net.dodian.uber.game.content.buttons.fletching.BowFletchingButtons)
        register(net.dodian.uber.game.content.buttons.duel.DuelOptionButtons)
        register(net.dodian.uber.game.content.buttons.appearance.AppearanceButtons)
        register(net.dodian.uber.game.content.buttons.magic.AutocastButtons)
        register(net.dodian.uber.game.content.buttons.magic.SpellbookToggleButtons)
        register(net.dodian.uber.game.content.buttons.ui.CloseInterfaceButtons)
        register(net.dodian.uber.game.content.buttons.ui.TabButtons)
        register(net.dodian.uber.game.content.buttons.settings.BossYellButtons)
        register(net.dodian.uber.game.content.buttons.travel.TravelMenuButtons)
        register(net.dodian.uber.game.content.buttons.party.PartyRoomButtons)
        register(YanilleHomeTeleportButton)
        register(SeersTeleportButton)
        register(ArdougneTeleportButton)
        register(CatherbyTeleportButton)
        register(LegendsGuildTeleportButton)
        register(TaverlyTeleportButton)
        register(FishingGuildTeleportButton)
        register(GnomeVillageTeleportButton)
        register(EdgevilleTeleportButton)
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
        val requiredInterfaceId = content.requiredInterfaceId
        if (requiredInterfaceId != -1 && client.activeInterfaceId != requiredInterfaceId) {
            client.send(RemoveInterfaces())
            return true
        }
        return try {
            content.onClick(client, buttonId)
        } catch (e: Exception) {
            logger.error("Error handling buttonId={} via {}", buttonId, content::class.java.name, e)
            false
        }
    }
}
