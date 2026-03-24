package net.dodian.uber.game.plugin

import net.dodian.uber.game.content.interfaces.appearance.AppearanceInterfaceButtons
import net.dodian.uber.game.content.interfaces.bank.BankInterfaceButtons
import net.dodian.uber.game.content.interfaces.combat.CombatInterfaceButtons
import net.dodian.uber.game.content.interfaces.crafting.CraftingInterfaceButtons
import net.dodian.uber.game.content.interfaces.dialogue.DialogueInterfaceButtons
import net.dodian.uber.game.content.interfaces.duel.DuelInterfaceButtons
import net.dodian.uber.game.content.interfaces.emotes.EmoteInterfaceButtons
import net.dodian.uber.game.content.interfaces.fletching.FletchingInterfaceButtons
import net.dodian.uber.game.content.interfaces.magic.MagicInterfaceButtons
import net.dodian.uber.game.content.interfaces.partyroom.PartyRoomInterfaceButtons
import net.dodian.uber.game.content.interfaces.prayer.PrayerInterfaceButtons
import net.dodian.uber.game.content.interfaces.quests.QuestInterfaceButtons
import net.dodian.uber.game.content.interfaces.rewards.RewardInterfaceButtons
import net.dodian.uber.game.content.interfaces.settings.SettingsInterfaceButtons
import net.dodian.uber.game.content.interfaces.skillguide.SkillGuideInterfaceButtons
import net.dodian.uber.game.content.interfaces.slots.SlotsInterfaceButtons
import net.dodian.uber.game.content.interfaces.smithing.SmithingInterfaceButtons
import net.dodian.uber.game.content.interfaces.trade.TradeInterfaceButtons
import net.dodian.uber.game.content.interfaces.travel.TravelInterfaceButtons
import net.dodian.uber.game.content.interfaces.ui.UiInterfaceButtons
import net.dodian.uber.game.content.items.admin.StaffToolItems
import net.dodian.uber.game.content.items.consumables.DrinkItems
import net.dodian.uber.game.content.items.consumables.FoodItems
import net.dodian.uber.game.content.items.consumables.PotionItems
import net.dodian.uber.game.content.items.cosmetics.ToyItems
import net.dodian.uber.game.content.items.equipment.RepairHintItems
import net.dodian.uber.game.content.items.events.EventInfoItems
import net.dodian.uber.game.content.items.events.EventPackageItems
import net.dodian.uber.game.content.items.herblore.GrimyHerbItems
import net.dodian.uber.game.content.items.herblore.HerbloreSuppliesItems
import net.dodian.uber.game.content.items.prayer.BuryBonesItems
import net.dodian.uber.game.content.items.rewards.LampRewardItems
import net.dodian.uber.game.content.items.runecrafting.RunePouchItems
import net.dodian.uber.game.content.items.slayer.SlayerGemItems
import net.dodian.uber.game.content.items.slayer.SlayerMaskItems
import net.dodian.uber.game.content.items.utility.GuideBookItems
import net.dodian.uber.game.content.objects.agility.BarbarianCourseObjectBindings
import net.dodian.uber.game.content.objects.agility.GnomeCourseObjectBindings
import net.dodian.uber.game.content.objects.agility.WerewolfCourseObjectBindings
import net.dodian.uber.game.content.objects.agility.WildernessCourseObjectBindings
import net.dodian.uber.game.content.objects.banking.BankBoothObjects
import net.dodian.uber.game.content.objects.banking.BankChestObjects
import net.dodian.uber.game.content.objects.cooking.RangeObjects
import net.dodian.uber.game.content.objects.crafting.ResourceFillingObjects
import net.dodian.uber.game.content.objects.crafting.SpinningWheelObjects
import net.dodian.uber.game.content.objects.doors.DoorToggleObjects
import net.dodian.uber.game.content.objects.events.PartyRoomObjectBindings
import net.dodian.uber.game.content.objects.farming.CompostBinObjects
import net.dodian.uber.game.content.objects.farming.FarmingPatchGuideObjects
import net.dodian.uber.game.content.objects.farming.FarmingPatchObjects
import net.dodian.uber.game.content.objects.mining.GemRocksObjectBindings
import net.dodian.uber.game.content.objects.mining.MiningRocksObjects
import net.dodian.uber.game.content.objects.mining.SpecialMiningObjectBindings
import net.dodian.uber.game.content.objects.prayer.AltarObjects
import net.dodian.uber.game.content.objects.runecrafting.RunecraftingObjectBindings
import net.dodian.uber.game.content.objects.smithing.AnvilObjects
import net.dodian.uber.game.content.objects.smithing.FurnaceObjects
import net.dodian.uber.game.content.objects.thieving.ChestObjects
import net.dodian.uber.game.content.objects.thieving.PlunderObjects
import net.dodian.uber.game.content.objects.thieving.StallObjects
import net.dodian.uber.game.content.objects.travel.LadderObjects
import net.dodian.uber.game.content.objects.travel.PassageObjects
import net.dodian.uber.game.content.objects.travel.StaircaseObjects
import net.dodian.uber.game.content.objects.travel.TeleportObjects
import net.dodian.uber.game.content.objects.travel.VerticalTeleportObjects
import net.dodian.uber.game.content.objects.travel.WebObstacleObjects
import net.dodian.uber.game.content.objects.woodcutting.WoodcuttingTreesObjects
import net.dodian.uber.game.event.bootstrap.EventBusProbeBootstrap
import net.dodian.uber.game.plugin.annotations.RegisterEventBootstrap
import net.dodian.uber.game.plugin.annotations.RegisterInterfaceButtons
import net.dodian.uber.game.plugin.annotations.RegisterItemContent
import net.dodian.uber.game.plugin.annotations.RegisterObjectContent

@RegisterInterfaceButtons(SkillGuideInterfaceButtons::class)
@RegisterInterfaceButtons(DialogueInterfaceButtons::class)
@RegisterInterfaceButtons(AppearanceInterfaceButtons::class)
@RegisterInterfaceButtons(CombatInterfaceButtons::class)
@RegisterInterfaceButtons(CraftingInterfaceButtons::class)
@RegisterInterfaceButtons(DuelInterfaceButtons::class)
@RegisterInterfaceButtons(EmoteInterfaceButtons::class)
@RegisterInterfaceButtons(FletchingInterfaceButtons::class)
@RegisterInterfaceButtons(SmithingInterfaceButtons::class)
@RegisterInterfaceButtons(BankInterfaceButtons::class)
@RegisterInterfaceButtons(PrayerInterfaceButtons::class)
@RegisterInterfaceButtons(MagicInterfaceButtons::class)
@RegisterInterfaceButtons(SettingsInterfaceButtons::class)
@RegisterInterfaceButtons(QuestInterfaceButtons::class)
@RegisterInterfaceButtons(PartyRoomInterfaceButtons::class)
@RegisterInterfaceButtons(RewardInterfaceButtons::class)
@RegisterInterfaceButtons(SlotsInterfaceButtons::class)
@RegisterInterfaceButtons(TradeInterfaceButtons::class)
@RegisterInterfaceButtons(TravelInterfaceButtons::class)
@RegisterInterfaceButtons(UiInterfaceButtons::class)
@RegisterObjectContent(AltarObjects::class)
@RegisterObjectContent(AnvilObjects::class)
@RegisterObjectContent(BarbarianCourseObjectBindings::class)
@RegisterObjectContent(BankBoothObjects::class)
@RegisterObjectContent(BankChestObjects::class)
@RegisterObjectContent(ChestObjects::class)
@RegisterObjectContent(CompostBinObjects::class)
@RegisterObjectContent(DoorToggleObjects::class)
@RegisterObjectContent(FarmingPatchObjects::class)
@RegisterObjectContent(FurnaceObjects::class)
@RegisterObjectContent(FarmingPatchGuideObjects::class)
@RegisterObjectContent(GemRocksObjectBindings::class)
@RegisterObjectContent(GnomeCourseObjectBindings::class)
@RegisterObjectContent(LadderObjects::class)
@RegisterObjectContent(MiningRocksObjects::class)
@RegisterObjectContent(PassageObjects::class)
@RegisterObjectContent(PartyRoomObjectBindings::class)
@RegisterObjectContent(PlunderObjects::class)
@RegisterObjectContent(RangeObjects::class)
@RegisterObjectContent(ResourceFillingObjects::class)
@RegisterObjectContent(RunecraftingObjectBindings::class)
@RegisterObjectContent(SpecialMiningObjectBindings::class)
@RegisterObjectContent(SpinningWheelObjects::class)
@RegisterObjectContent(StaircaseObjects::class)
@RegisterObjectContent(StallObjects::class)
@RegisterObjectContent(TeleportObjects::class)
@RegisterObjectContent(VerticalTeleportObjects::class)
@RegisterObjectContent(WebObstacleObjects::class)
@RegisterObjectContent(WerewolfCourseObjectBindings::class)
@RegisterObjectContent(WildernessCourseObjectBindings::class)
@RegisterObjectContent(WoodcuttingTreesObjects::class)
@RegisterItemContent(StaffToolItems::class)
@RegisterItemContent(BuryBonesItems::class)
@RegisterItemContent(RunePouchItems::class)
@RegisterItemContent(SlayerGemItems::class)
@RegisterItemContent(SlayerMaskItems::class)
@RegisterItemContent(DrinkItems::class)
@RegisterItemContent(FoodItems::class)
@RegisterItemContent(PotionItems::class)
@RegisterItemContent(GrimyHerbItems::class)
@RegisterItemContent(HerbloreSuppliesItems::class)
@RegisterItemContent(LampRewardItems::class)
@RegisterItemContent(GuideBookItems::class)
@RegisterItemContent(ToyItems::class)
@RegisterItemContent(RepairHintItems::class)
@RegisterItemContent(EventInfoItems::class)
@RegisterItemContent(EventPackageItems::class)
@RegisterEventBootstrap(module = EventBusProbeBootstrap::class)
object PluginModuleDeclarations
