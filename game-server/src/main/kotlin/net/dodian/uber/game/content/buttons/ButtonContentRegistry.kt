package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.content.buttons.appearance.AppearanceConfirmButtons
import net.dodian.uber.game.content.buttons.banking.BankDepositButtons
import net.dodian.uber.game.content.buttons.banking.BankInterfaceButtons
import net.dodian.uber.game.content.buttons.crafting.LeatherCraftButtons
import net.dodian.uber.game.content.buttons.crafting.GlassCraftButtons
import net.dodian.uber.game.content.buttons.crafting.ProductionAmountButtons
import net.dodian.uber.game.content.buttons.crafting.SmeltingButtons
import net.dodian.uber.game.content.buttons.crafting.TanningButtons
import net.dodian.uber.game.content.buttons.dialogue.NpcDialogueStateButtons
import net.dodian.uber.game.content.buttons.dueling.DuelRuleButtons
import net.dodian.uber.game.content.buttons.dueling.DuelConfirmButtons
import net.dodian.uber.game.content.buttons.emotes.SpecialEmoteButtons
import net.dodian.uber.game.content.buttons.fletching.BowFletchingButtons
import net.dodian.uber.game.content.buttons.minigames.SlotsButtons
import net.dodian.uber.game.content.buttons.magic.AutocastButtons
import net.dodian.uber.game.content.buttons.magic.SpellbookToggleButtons
import net.dodian.uber.game.content.buttons.magic.teleports.ArdougneTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.CatherbyTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.EdgevilleTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.FishingGuildTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.GnomeVillageTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.LegendsGuildTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.SeersTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.TaverlyTeleportButton
import net.dodian.uber.game.content.buttons.magic.teleports.YanilleHomeTeleportButton
import net.dodian.uber.game.content.buttons.partyroom.PartyRoomDepositButtons
import net.dodian.uber.game.content.buttons.rewards.RewardLampButtons
import net.dodian.uber.game.content.buttons.settings.BossYellButtons
import net.dodian.uber.game.content.buttons.settings.PinHelpButtons
import net.dodian.uber.game.content.buttons.settings.SettingsTabButtons
import net.dodian.uber.game.content.buttons.skills.SkillGuideButtons
import net.dodian.uber.game.content.buttons.trade.TradeConfirmButtons
import net.dodian.uber.game.content.buttons.travel.TravelMenuButtons
import net.dodian.uber.game.content.buttons.ui.CloseInterfaceButtons
import net.dodian.uber.game.content.buttons.ui.IgnoredButtons
import net.dodian.uber.game.content.buttons.ui.LogoutButtons
import net.dodian.uber.game.content.buttons.ui.MorphButtons
import net.dodian.uber.game.content.buttons.ui.QuestTabButtons
import net.dodian.uber.game.content.buttons.ui.RunButtons
import net.dodian.uber.game.content.buttons.ui.SidebarButtons
import net.dodian.uber.game.content.buttons.ui.TabInterfaceButtons
import net.dodian.uber.game.content.buttons.combat.CombatStyleButtons
import net.dodian.uber.game.content.buttons.dialogue.DialogueOptionButtons
import net.dodian.uber.game.model.entity.player.Client
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
        register(NpcDialogueStateButtons)
        register(BankDepositButtons)
        register(BankInterfaceButtons)
        register(ProductionAmountButtons)
        register(GlassCraftButtons)
        register(SmeltingButtons)
        register(LeatherCraftButtons)
        register(TanningButtons)
        register(BowFletchingButtons)
        register(DuelRuleButtons)
        register(DuelConfirmButtons)
        register(AppearanceConfirmButtons)
        register(AutocastButtons)
        register(SpellbookToggleButtons)
        register(CombatStyleButtons)
        register(CloseInterfaceButtons)
        register(TabInterfaceButtons)
        register(RunButtons)
        register(LogoutButtons)
        register(SidebarButtons)
        register(QuestTabButtons)
        register(BossYellButtons)
        register(DialogueOptionButtons)
        register(RewardLampButtons)
        register(TradeConfirmButtons)
        register(SkillGuideButtons)
        register(SpecialEmoteButtons)
        register(MorphButtons)
        register(SlotsButtons)
        register(IgnoredButtons)
        register(TravelMenuButtons)
        register(PartyRoomDepositButtons)
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
