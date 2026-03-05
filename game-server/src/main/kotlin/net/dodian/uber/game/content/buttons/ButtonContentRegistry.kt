package net.dodian.uber.game.content.buttons

import net.dodian.uber.game.content.buttons.appearance.AppearanceConfirmButtons
import net.dodian.uber.game.content.buttons.banking.BankDepositButtons
import net.dodian.uber.game.content.buttons.banking.BankInterfaceButtons
import net.dodian.uber.game.content.buttons.combat.CombatStyleButtons
import net.dodian.uber.game.content.buttons.crafting.GlassCraftButtons
import net.dodian.uber.game.content.buttons.crafting.LeatherCraftButtons
import net.dodian.uber.game.content.buttons.crafting.ProductionAmountButtons
import net.dodian.uber.game.content.buttons.crafting.SmeltingButtons
import net.dodian.uber.game.content.buttons.crafting.TanningButtons
import net.dodian.uber.game.content.buttons.dialogue.DialogueOptionButtons
import net.dodian.uber.game.content.buttons.dialogue.NpcDialogueStateButtons
import net.dodian.uber.game.content.buttons.dueling.DuelConfirmButtons
import net.dodian.uber.game.content.buttons.dueling.DuelOfferRuleButtons
import net.dodian.uber.game.content.buttons.dueling.DuelRuleButtons
import net.dodian.uber.game.content.buttons.emotes.BasicEmoteButtons
import net.dodian.uber.game.content.buttons.emotes.SpecialEmoteButtons
import net.dodian.uber.game.content.buttons.fletching.BowFletchingButtons
import net.dodian.uber.game.content.buttons.magic.AutocastButtons
import net.dodian.uber.game.content.buttons.magic.SpellbookToggleButtons
import net.dodian.uber.game.content.buttons.magic.teleports.AncientTeleportButtons
import net.dodian.uber.game.content.buttons.magic.teleports.NormalTeleportButtons
import net.dodian.uber.game.content.buttons.minigames.SlotsButtons
import net.dodian.uber.game.content.buttons.partyroom.PartyRoomDepositButtons
import net.dodian.uber.game.content.buttons.prayer.PrayerButtons
import net.dodian.uber.game.content.buttons.quests.QuestMenuButtons
import net.dodian.uber.game.content.buttons.rewards.RewardLampButtons
import net.dodian.uber.game.content.buttons.settings.BossYellButtons
import net.dodian.uber.game.content.buttons.settings.PinHelpButtons
import net.dodian.uber.game.content.buttons.settings.SettingsTabButtons
import net.dodian.uber.game.content.buttons.skillguide.AgilitySkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.AttackSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.CookingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.CraftingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.DefenceSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.FarmingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.FiremakingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.FishingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.FletchingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.HerbloreSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.HitpointsSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.MagicSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.MiningSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.PrayerSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.RangedSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.RunecraftingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.SkillGuideSubTabButtons
import net.dodian.uber.game.content.buttons.skillguide.SlayerSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.SmithingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.StrengthSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.ThievingSkillGuideButtons
import net.dodian.uber.game.content.buttons.skillguide.WoodcuttingSkillGuideButtons
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
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object ButtonContentRegistry {
    private val logger = LoggerFactory.getLogger(ButtonContentRegistry::class.java)

    private val bootstrapped = AtomicBoolean(false)
    private val definitions = mutableListOf<ButtonContent>()

    @Volatile
    private var byButtonId: Array<ButtonContent?> = emptyArray()

    @JvmStatic
    fun bootstrap() {
        if (bootstrapped.get()) {
            return
        }
        synchronized(this) {
            if (bootstrapped.get()) {
                return
            }
            definitions += builtinContents()
            rebuildLookupLocked()
            bootstrapped.set(true)
        }
    }

    fun ensureLoaded() {
        bootstrap()
    }

    fun register(content: ButtonContent) {
        synchronized(this) {
            definitions += content
            if (bootstrapped.get()) {
                rebuildLookupLocked()
            }
        }
    }

    fun tryHandle(client: Client, buttonId: Int): Boolean {
        bootstrap()
        val table = byButtonId
        if (buttonId < 0 || buttonId >= table.size) {
            return false
        }
        val content = table[buttonId] ?: return false
        val requiredInterfaceId = content.requiredInterfaceId
        if (requiredInterfaceId != -1 && client.activeInterfaceId != requiredInterfaceId) {
            client.send(RemoveInterfaces())
            return true
        }
        val startNs = System.nanoTime()
        return try {
            content.onClick(client, buttonId)
        } catch (e: Exception) {
            logger.error("Error handling buttonId={} via {}", buttonId, content::class.java.name, e)
            false
        } finally {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            if (elapsedMs >= 100L) {
                logger.warn(
                    "Slow button: buttonId={} handler={} iface={} player={} {}ms",
                    buttonId,
                    content::class.java.name,
                    client.activeInterfaceId,
                    client.playerName,
                    elapsedMs
                )
            }
        }
    }

    private fun rebuildLookupLocked() {
        val maxButtonId = definitions.asSequence().flatMap { it.buttonIds.asSequence() }.maxOrNull() ?: -1
        val rebuilt = arrayOfNulls<ButtonContent>(maxButtonId + 1)
        for (content in definitions) {
            for (buttonId in content.buttonIds) {
                val existing = rebuilt[buttonId]
                if (existing != null && existing !== content) {
                    logger.error(
                        "Duplicate ButtonContent for buttonId={} (existing={}, new={})",
                        buttonId,
                        existing::class.java.name,
                        content::class.java.name,
                    )
                } else {
                    rebuilt[buttonId] = content
                }
            }
        }
        byButtonId = rebuilt
    }

    private fun builtinContents(): List<ButtonContent> =
        listOf(
            PinHelpButtons,
            SettingsTabButtons,
            QuestMenuButtons,
            PrayerButtons,
            NpcDialogueStateButtons,
            BankDepositButtons,
            BankInterfaceButtons,
            ProductionAmountButtons,
            GlassCraftButtons,
            SmeltingButtons,
            LeatherCraftButtons,
            TanningButtons,
            BowFletchingButtons,
            DuelOfferRuleButtons,
            DuelRuleButtons,
            DuelConfirmButtons,
            AppearanceConfirmButtons,
            AutocastButtons,
            SpellbookToggleButtons,
            CombatStyleButtons,
            CloseInterfaceButtons,
            TabInterfaceButtons,
            RunButtons,
            LogoutButtons,
            SidebarButtons,
            QuestTabButtons,
            BossYellButtons,
            DialogueOptionButtons,
            RewardLampButtons,
            TradeConfirmButtons,
            AttackSkillGuideButtons,
            HitpointsSkillGuideButtons,
            MiningSkillGuideButtons,
            StrengthSkillGuideButtons,
            AgilitySkillGuideButtons,
            DefenceSkillGuideButtons,
            RangedSkillGuideButtons,
            PrayerSkillGuideButtons,
            ThievingSkillGuideButtons,
            HerbloreSkillGuideButtons,
            CraftingSkillGuideButtons,
            SmithingSkillGuideButtons,
            WoodcuttingSkillGuideButtons,
            MagicSkillGuideButtons,
            FiremakingSkillGuideButtons,
            CookingSkillGuideButtons,
            RunecraftingSkillGuideButtons,
            FletchingSkillGuideButtons,
            FishingSkillGuideButtons,
            SlayerSkillGuideButtons,
            FarmingSkillGuideButtons,
            SkillGuideSubTabButtons,
            BasicEmoteButtons,
            SpecialEmoteButtons,
            MorphButtons,
            SlotsButtons,
            IgnoredButtons,
            TravelMenuButtons,
            PartyRoomDepositButtons,
            AncientTeleportButtons,
            NormalTeleportButtons,
        )
}
