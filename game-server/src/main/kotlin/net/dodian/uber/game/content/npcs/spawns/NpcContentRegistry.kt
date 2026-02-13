package net.dodian.uber.game.content.npcs.spawns

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcContentDefinition>()
    private val definitions = mutableListOf<NpcContentDefinition>()

    fun ensureLoaded() {
        if (!loaded.compareAndSet(false, true)) return
        register(
            NpcContentDefinition(
                name = "Aubury",
                npcIds = Aubury.npcIds,
                ownsSpawnDefinitions = true,
                entries = Aubury.entries,
                onFirstClick = Aubury::onFirstClick,
                onSecondClick = Aubury::onSecondClick,
                onThirdClick = Aubury::onThirdClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Banker",
                npcIds = Banker.npcIds,
                entries = Banker.entries,
                onFirstClick = Banker::onFirstClick,
                onSecondClick = Banker::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Monk",
                npcIds = Monk.npcIds,
                entries = Monk.entries,
                onFirstClick = Monk::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "CustomsOfficer",
                npcIds = CustomsOfficer.npcIds,
                entries = CustomsOfficer.entries,
                onFirstClick = CustomsOfficer::onFirstClick,
                onSecondClick = CustomsOfficer::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Mac",
                npcIds = Mac.npcIds,
                entries = Mac.entries,
                onFirstClick = Mac::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Saniboch",
                npcIds = Saniboch.npcIds,
                entries = Saniboch.entries,
                onFirstClick = Saniboch::onFirstClick,
                onSecondClick = Saniboch::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "BabaYaga",
                npcIds = BabaYaga.npcIds,
                entries = BabaYaga.entries,
                onFirstClick = BabaYaga::onFirstClick,
                onSecondClick = BabaYaga::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "TzhaarMejJal",
                npcIds = TzhaarMejJal.npcIds,
                entries = TzhaarMejJal.entries,
                onFirstClick = TzhaarMejJal::onFirstClick,
                onSecondClick = TzhaarMejJal::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "GnomeTrainer",
                npcIds = GnomeTrainer.npcIds,
                entries = GnomeTrainer.entries,
                onFirstClick = GnomeTrainer::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "PiratePete",
                npcIds = PiratePete.npcIds,
                entries = PiratePete.entries,
                onFirstClick = PiratePete::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Sheep",
                npcIds = intArrayOf(2794),
                entries = Sheep.entries,
                onFirstClick = Sheep::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "PartyAnnouncer",
                npcIds = intArrayOf(5792),
                entries = PartyAnnouncer.entries,
                onFirstClick = PartyAnnouncer::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "PopulationAnnouncer",
                npcIds = intArrayOf(3306),
                entries = PopulationAnnouncer.entries,
                onFirstClick = PopulationAnnouncer::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Zogre",
                npcIds = intArrayOf(2053),
                entries = Zogre.entries,
                onFirstClick = Zogre::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "LegendsGuard",
                npcIds = intArrayOf(3951),
                entries = LegendsGuard.entries,
                onFirstClick = LegendsGuard::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "BeginnerStore",
                npcIds = intArrayOf(3640),
                entries = BeginnerStore.entries,
                onFirstClick = BeginnerStore::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "PremiumStore",
                npcIds = intArrayOf(556),
                entries = PremiumStore.entries,
                onFirstClick = PremiumStore::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Wydin",
                npcIds = intArrayOf(557),
                entries = Wydin.entries,
                onFirstClick = Wydin::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Dori",
                npcIds = intArrayOf(4808),
                entries = Dori.entries,
                onFirstClick = Dori::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "AliHag",
                npcIds = intArrayOf(3541),
                entries = AliHag.entries,
                onFirstClick = AliHag::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "AgilityWerewolfMaster",
                npcIds = AgilityWerewolfMaster.npcIds,
                onFirstClick = AgilityWerewolfMaster::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "UnknownNpc683",
                npcIds = UnknownNpc683.npcIds,
                onFirstClick = UnknownNpc683::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "CaptainTobias",
                npcIds = CaptainTobias.npcIds,
                onFirstClick = CaptainTobias::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "DukeHoracio",
                npcIds = DukeHoracio.npcIds,
                onFirstClick = DukeHoracio::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "PartyPete",
                npcIds = PartyPete.npcIds,
                onFirstClick = PartyPete::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "ShopKeeper",
                npcIds = ShopKeeper.npcIds,
                entries = ShopKeeper.entries,
                onFirstClick = ShopKeeper::onFirstClick,
                onSecondClick = ShopKeeper::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "ShopAssistant",
                npcIds = ShopAssistant.npcIds,
                onSecondClick = ShopAssistant::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Cow",
                npcIds = Cow.npcIds,
                entries = Cow.entries,
                onFirstClick = Cow::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "SurvivalExpert",
                npcIds = SurvivalExpert.npcIds,
                onFirstClick = SurvivalExpert::onFirstClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Farmer",
                npcIds = Farmer.npcIds,
                entries = Farmer.entries,
                onSecondClick = Farmer::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "MasterFarmer",
                npcIds = MasterFarmer.npcIds,
                entries = MasterFarmer.entries,
                onSecondClick = MasterFarmer::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Sedridor",
                npcIds = Sedridor.npcIds,
                entries = Sedridor.entries,
                onSecondClick = Sedridor::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "WizardCromperty",
                npcIds = WizardCromperty.npcIds,
                onSecondClick = WizardCromperty::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "WizardDistentor",
                npcIds = WizardDistentor.npcIds,
                onSecondClick = WizardDistentor::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Ian",
                npcIds = Ian.npcIds,
                entries = Ian.entries,
                onFirstClick = Ian::onFirstClick,
                onSecondClick = Ian::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "ThievingSkillcapeShop",
                npcIds = ThievingSkillcapeShop.npcIds,
                entries = ThievingSkillcapeShop.entries,
                onSecondClick = ThievingSkillcapeShop::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Wolfman",
                npcIds = Wolfman.npcIds,
                entries = Wolfman.entries,
                onSecondClick = Wolfman::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Peksa",
                npcIds = Peksa.npcIds,
                onSecondClick = Peksa::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Rufu",
                npcIds = Rufu.npcIds,
                entries = Rufu.entries,
                onSecondClick = Rufu::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Nathifa",
                npcIds = Nathifa.npcIds,
                entries = Nathifa.entries,
                onSecondClick = Nathifa::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Horvik",
                npcIds = Horvik.npcIds,
                entries = Horvik.entries,
                onSecondClick = Horvik::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "BowArrowSalesman",
                npcIds = BowArrowSalesman.npcIds,
                entries = BowArrowSalesman.entries,
                onSecondClick = BowArrowSalesman::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Gerrant",
                npcIds = Gerrant.npcIds,
                entries = Gerrant.entries,
                onSecondClick = Gerrant::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Tanner",
                npcIds = Tanner.npcIds,
                entries = Tanner.entries,
                onFirstClick = Tanner::onFirstClick,
                onSecondClick = Tanner::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "ArmourSalesman",
                npcIds = ArmourSalesman.npcIds,
                entries = ArmourSalesman.entries,
                onSecondClick = ArmourSalesman::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Shantay",
                npcIds = Shantay.npcIds,
                entries = Shantay.entries,
                onSecondClick = Shantay::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Mazchna",
                npcIds = Mazchna.npcIds,
                entries = Mazchna.entries,
                onFirstClick = Mazchna::onFirstClick,
                onSecondClick = Mazchna::onSecondClick,
                onThirdClick = Mazchna::onThirdClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Vannaka",
                npcIds = Vannaka.npcIds,
                entries = Vannaka.entries,
                onFirstClick = Vannaka::onFirstClick,
                onSecondClick = Vannaka::onSecondClick,
                onThirdClick = Vannaka::onThirdClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Duradel",
                npcIds = Duradel.npcIds,
                entries = Duradel.entries,
                onFirstClick = Duradel::onFirstClick,
                onSecondClick = Duradel::onSecondClick,
                onThirdClick = Duradel::onThirdClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Jatix",
                npcIds = Jatix.npcIds,
                entries = Jatix.entries,
                onFirstClick = Jatix::onFirstClick,
                onSecondClick = Jatix::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Zahur",
                npcIds = Zahur.npcIds,
                entries = Zahur.entries,
                onFirstClick = Zahur::onFirstClick,
                onSecondClick = Zahur::onSecondClick,
                onThirdClick = Zahur::onThirdClick,
                onFourthClick = Zahur::onFourthClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "RugMerchant",
                npcIds = RugMerchant.npcIds,
                entries = RugMerchant.entries,
                onFirstClick = RugMerchant::onFirstClick,
                onSecondClick = RugMerchant::onSecondClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "Turael",
                npcIds = Turael.npcIds,
                onThirdClick = Turael::onThirdClick,
            ),
        )
        register(
            NpcContentDefinition(
                name = "MakeoverMage",
                npcIds = MakeoverMage.npcIds,
                ownsSpawnDefinitions = true,
                entries = MakeoverMage.entries,
                onFirstClick = MakeoverMage::onFirstClick,
                onThirdClick = MakeoverMage::onThirdClick,
            ),
        )
    }

    fun register(content: NpcContentDefinition) {
        val localDuplicates = content.npcIds.groupBy { it }.filterValues { it.size > 1 }.keys
        require(localDuplicates.isEmpty()) {
            "Duplicate npcIds in ${content.name}: ${localDuplicates.sorted()}"
        }

        val duplicateNpcIds = content.npcIds.filter { byNpcId.containsKey(it) }.distinct().sorted()
        require(duplicateNpcIds.isEmpty()) {
            val details = duplicateNpcIds.joinToString(",") { npcId ->
                val existing = byNpcId[npcId]
                "$npcId(existing=${existing?.name})"
            }
            "Duplicate NpcContent registration for ${content.name}: $details"
        }

        definitions += content
        for (npcId in content.npcIds) {
            byNpcId[npcId] = content
        }

        logger.debug(
            "Registered NpcContent {} for npcIds={}",
            content.name,
            content.npcIds.joinToString(","),
        )
    }

    @JvmStatic
    fun get(npcId: Int): NpcContentDefinition? {
        ensureLoaded()
        return byNpcId[npcId]
    }

    @JvmStatic
    fun allSpawns(): List<NpcSpawnDef> {
        ensureLoaded()
        return definitions.flatMap { it.entries }
    }

    @JvmStatic
    fun spawnSourceNpcIds(): Set<Int> {
        ensureLoaded()
        return definitions
            .asSequence()
            .filter { it.ownsSpawnDefinitions }
            .flatMap { it.npcIds.asSequence() }
            .toSet()
    }

    internal fun clearForTests() {
        loaded.set(true)
        byNpcId.clear()
        definitions.clear()
    }

    internal fun resetForTests() {
        loaded.set(false)
        byNpcId.clear()
        definitions.clear()
    }
}
