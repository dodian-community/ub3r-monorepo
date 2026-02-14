package net.dodian.uber.game.content.npcs.spawns

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object NpcContentRegistry {
    private val logger = LoggerFactory.getLogger(NpcContentRegistry::class.java)

    private val loaded = AtomicBoolean(false)
    private val byNpcId = ConcurrentHashMap<Int, NpcContentDefinition>()
    private val definitions = mutableListOf<NpcContentDefinition>()

    private fun registerNpc(
        name: String,
        npcIds: IntArray,
        entries: List<NpcSpawnDef> = emptyList(),
        ownsSpawnDefinitions: Boolean = false,
        onFirstClick: NpcClickHandler = NO_CLICK_HANDLER,
        onSecondClick: NpcClickHandler = NO_CLICK_HANDLER,
        onThirdClick: NpcClickHandler = NO_CLICK_HANDLER,
        onFourthClick: NpcClickHandler = NO_CLICK_HANDLER,
        onAttack: NpcClickHandler = NO_CLICK_HANDLER,
    ): NpcContentDefinition = NpcContentDefinition(
        name = name,
        npcIds = npcIds,
        ownsSpawnDefinitions = ownsSpawnDefinitions,
        entries = entries,
        onFirstClick = onFirstClick,
        onSecondClick = onSecondClick,
        onThirdClick = onThirdClick,
        onFourthClick = onFourthClick,
        onAttack = onAttack,
    )

    fun ensureLoaded() {
        if (loaded.get()) return
        synchronized(this) {
            if (loaded.get()) return
            val pending = mutableListOf<NpcContentDefinition>()
            pending += registerNpc(
                name = "Aubury",
                npcIds = Aubury.npcIds,
                ownsSpawnDefinitions = true,
                entries = Aubury.entries,
                onFirstClick = Aubury::onFirstClick,
                onSecondClick = Aubury::onSecondClick,
                onThirdClick = Aubury::onThirdClick,
        )
            pending += registerNpc(
                name = "Banker",
                npcIds = Banker.npcIds,
                entries = Banker.entries,
                onFirstClick = Banker::onFirstClick,
                onSecondClick = Banker::onSecondClick,
        )
            pending += registerNpc(
                name = "Monk",
                npcIds = Monk.npcIds,
                entries = Monk.entries,
                onFirstClick = Monk::onFirstClick,
        )
            pending += registerNpc(
                name = "CustomsOfficer",
                npcIds = CustomsOfficer.npcIds,
                entries = CustomsOfficer.entries,
                onFirstClick = CustomsOfficer::onFirstClick,
                onSecondClick = CustomsOfficer::onSecondClick,
        )
            pending += registerNpc(
                name = "Mac",
                npcIds = Mac.npcIds,
                entries = Mac.entries,
                onFirstClick = Mac::onFirstClick,
        )
            pending += registerNpc(
                name = "Saniboch",
                npcIds = Saniboch.npcIds,
                entries = Saniboch.entries,
                onFirstClick = Saniboch::onFirstClick,
                onSecondClick = Saniboch::onSecondClick,
        )
            pending += registerNpc(
                name = "BabaYaga",
                npcIds = BabaYaga.npcIds,
                entries = BabaYaga.entries,
                onFirstClick = BabaYaga::onFirstClick,
                onSecondClick = BabaYaga::onSecondClick,
        )
            pending += registerNpc(
                name = "TzhaarMejJal",
                npcIds = TzhaarMejJal.npcIds,
                entries = TzhaarMejJal.entries,
                onFirstClick = TzhaarMejJal::onFirstClick,
                onSecondClick = TzhaarMejJal::onSecondClick,
        )
            pending += registerNpc(
                name = "GnomeTrainer",
                npcIds = GnomeTrainer.npcIds,
                entries = GnomeTrainer.entries,
                onFirstClick = GnomeTrainer::onFirstClick,
        )
            pending += registerNpc(
                name = "PiratePete",
                npcIds = PiratePete.npcIds,
                entries = PiratePete.entries,
                onFirstClick = PiratePete::onFirstClick,
        )
            pending += registerNpc(
                name = "Sheep",
                npcIds = Sheep.npcIds,
                entries = Sheep.entries,
                onFirstClick = Sheep::onFirstClick,
        )
            pending += registerNpc(
                name = "PartyAnnouncer",
                npcIds = PartyAnnouncer.npcIds,
                entries = PartyAnnouncer.entries,
                onFirstClick = PartyAnnouncer::onFirstClick,
        )
            pending += registerNpc(
                name = "PopulationAnnouncer",
                npcIds = PopulationAnnouncer.npcIds,
                entries = PopulationAnnouncer.entries,
                onFirstClick = PopulationAnnouncer::onFirstClick,
        )
            pending += registerNpc(
                name = "Zogre",
                npcIds = Zogre.npcIds,
                entries = Zogre.entries,
                onFirstClick = Zogre::onFirstClick,
        )
            pending += registerNpc(
                name = "LegendsGuard",
                npcIds = LegendsGuard.npcIds,
                entries = LegendsGuard.entries,
                onFirstClick = LegendsGuard::onFirstClick,
        )
            pending += registerNpc(
                name = "BeginnerStore",
                npcIds = BeginnerStore.npcIds,
                entries = BeginnerStore.entries,
                onFirstClick = BeginnerStore::onFirstClick,
        )
            pending += registerNpc(
                name = "PremiumStore",
                npcIds = PremiumStore.npcIds,
                entries = PremiumStore.entries,
                onFirstClick = PremiumStore::onFirstClick,
        )
            pending += registerNpc(
                name = "Wydin",
                npcIds = Wydin.npcIds,
                entries = Wydin.entries,
                onFirstClick = Wydin::onFirstClick,
        )
            pending += registerNpc(
                name = "Dori",
                npcIds = Dori.npcIds,
                entries = Dori.entries,
                onFirstClick = Dori::onFirstClick,
        )
            pending += registerNpc(
                name = "AliHag",
                npcIds = AliHag.npcIds,
                entries = AliHag.entries,
                onFirstClick = AliHag::onFirstClick,
        )
            pending += registerNpc(
                name = "AgilityWerewolfMaster",
                npcIds = AgilityWerewolfMaster.npcIds,
                onFirstClick = AgilityWerewolfMaster::onFirstClick,
        )
            pending += registerNpc(
                name = "UnknownNpc683",
                npcIds = UnknownNpc683.npcIds,
                onFirstClick = UnknownNpc683::onFirstClick,
        )
            pending += registerNpc(
                name = "CaptainTobias",
                npcIds = CaptainTobias.npcIds,
                onFirstClick = CaptainTobias::onFirstClick,
        )
            pending += registerNpc(
                name = "DukeHoracio",
                npcIds = DukeHoracio.npcIds,
                onFirstClick = DukeHoracio::onFirstClick,
        )
            pending += registerNpc(
                name = "PartyPete",
                npcIds = PartyPete.npcIds,
                onFirstClick = PartyPete::onFirstClick,
        )
            pending += registerNpc(
                name = "ShopKeeper",
                npcIds = ShopKeeper.npcIds,
                entries = ShopKeeper.entries,
                onFirstClick = ShopKeeper::onFirstClick,
                onSecondClick = ShopKeeper::onSecondClick,
        )
            pending += registerNpc(
                name = "ShopAssistant",
                npcIds = ShopAssistant.npcIds,
                onSecondClick = ShopAssistant::onSecondClick,
        )
            pending += registerNpc(
                name = "Cow",
                npcIds = Cow.npcIds,
                entries = Cow.entries,
                onFirstClick = Cow::onFirstClick,
        )
            pending += registerNpc(
                name = "SurvivalExpert",
                npcIds = SurvivalExpert.npcIds,
                onFirstClick = SurvivalExpert::onFirstClick,
        )
            pending += registerNpc(
                name = "Farmer",
                npcIds = Farmer.npcIds,
                entries = Farmer.entries,
                onSecondClick = Farmer::onSecondClick,
        )
            pending += registerNpc(
                name = "MasterFarmer",
                npcIds = MasterFarmer.npcIds,
                entries = MasterFarmer.entries,
                onSecondClick = MasterFarmer::onSecondClick,
        )
            pending += registerNpc(
                name = "Sedridor",
                npcIds = Sedridor.npcIds,
                entries = Sedridor.entries,
                onSecondClick = Sedridor::onSecondClick,
        )
            pending += registerNpc(
                name = "WizardCromperty",
                npcIds = WizardCromperty.npcIds,
                onSecondClick = WizardCromperty::onSecondClick,
        )
            pending += registerNpc(
                name = "WizardDistentor",
                npcIds = WizardDistentor.npcIds,
                onSecondClick = WizardDistentor::onSecondClick,
        )
            pending += registerNpc(
                name = "Ian",
                npcIds = Ian.npcIds,
                entries = Ian.entries,
                onFirstClick = Ian::onFirstClick,
                onSecondClick = Ian::onSecondClick,
        )
            pending += registerNpc(
                name = "ThievingSkillcapeShop",
                npcIds = ThievingSkillcapeShop.npcIds,
                entries = ThievingSkillcapeShop.entries,
                onSecondClick = ThievingSkillcapeShop::onSecondClick,
        )
            pending += registerNpc(
                name = "Wolfman",
                npcIds = Wolfman.npcIds,
                entries = Wolfman.entries,
                onSecondClick = Wolfman::onSecondClick,
        )
            pending += registerNpc(
                name = "Peksa",
                npcIds = Peksa.npcIds,
                onSecondClick = Peksa::onSecondClick,
        )
            pending += registerNpc(
                name = "Rufu",
                npcIds = Rufu.npcIds,
                entries = Rufu.entries,
                onSecondClick = Rufu::onSecondClick,
        )
            pending += registerNpc(
                name = "Nathifa",
                npcIds = Nathifa.npcIds,
                entries = Nathifa.entries,
                onSecondClick = Nathifa::onSecondClick,
        )
            pending += registerNpc(
                name = "Horvik",
                npcIds = Horvik.npcIds,
                entries = Horvik.entries,
                onSecondClick = Horvik::onSecondClick,
        )
            pending += registerNpc(
                name = "BowArrowSalesman",
                npcIds = BowArrowSalesman.npcIds,
                entries = BowArrowSalesman.entries,
                onSecondClick = BowArrowSalesman::onSecondClick,
        )
            pending += registerNpc(
                name = "Gerrant",
                npcIds = Gerrant.npcIds,
                entries = Gerrant.entries,
                onSecondClick = Gerrant::onSecondClick,
        )
            pending += registerNpc(
                name = "Tanner",
                npcIds = Tanner.npcIds,
                entries = Tanner.entries,
                onFirstClick = Tanner::onFirstClick,
                onSecondClick = Tanner::onSecondClick,
        )
            pending += registerNpc(
                name = "ArmourSalesman",
                npcIds = ArmourSalesman.npcIds,
                entries = ArmourSalesman.entries,
                onSecondClick = ArmourSalesman::onSecondClick,
        )
            pending += registerNpc(
                name = "Shantay",
                npcIds = Shantay.npcIds,
                entries = Shantay.entries,
                onSecondClick = Shantay::onSecondClick,
        )
            pending += registerNpc(
                name = "Mazchna",
                npcIds = Mazchna.npcIds,
                entries = Mazchna.entries,
                onFirstClick = Mazchna::onFirstClick,
                onSecondClick = Mazchna::onSecondClick,
                onThirdClick = Mazchna::onThirdClick,
        )
            pending += registerNpc(
                name = "Vannaka",
                npcIds = Vannaka.npcIds,
                entries = Vannaka.entries,
                onFirstClick = Vannaka::onFirstClick,
                onSecondClick = Vannaka::onSecondClick,
                onThirdClick = Vannaka::onThirdClick,
        )
            pending += registerNpc(
                name = "Duradel",
                npcIds = Duradel.npcIds,
                entries = Duradel.entries,
                onFirstClick = Duradel::onFirstClick,
                onSecondClick = Duradel::onSecondClick,
                onThirdClick = Duradel::onThirdClick,
        )
            pending += registerNpc(
                name = "Jatix",
                npcIds = Jatix.npcIds,
                entries = Jatix.entries,
                onFirstClick = Jatix::onFirstClick,
                onSecondClick = Jatix::onSecondClick,
        )
            pending += registerNpc(
                name = "Zahur",
                npcIds = Zahur.npcIds,
                entries = Zahur.entries,
                onFirstClick = Zahur::onFirstClick,
                onSecondClick = Zahur::onSecondClick,
                onThirdClick = Zahur::onThirdClick,
                onFourthClick = Zahur::onFourthClick,
        )
            pending += registerNpc(
                name = "RugMerchant",
                npcIds = RugMerchant.npcIds,
                entries = RugMerchant.entries,
                onFirstClick = RugMerchant::onFirstClick,
                onSecondClick = RugMerchant::onSecondClick,
        )
            pending += registerNpc(
                name = "Turael",
                npcIds = Turael.npcIds,
                onThirdClick = Turael::onThirdClick,
        )
            pending += registerNpc(
                name = "MakeoverMage",
                npcIds = MakeoverMage.npcIds,
                ownsSpawnDefinitions = true,
                entries = MakeoverMage.entries,
                onFirstClick = MakeoverMage::onFirstClick,
                onThirdClick = MakeoverMage::onThirdClick,
        )
            pending
                .sortedBy { it.name }
                .forEach(::register)
            loaded.set(true)
        }
    }

    fun register(content: NpcContentDefinition) {
        val localDuplicates = content.npcIds.groupBy { it }.filterValues { it.size > 1 }.keys
        require(localDuplicates.isEmpty()) {
            "Duplicate npcIds in ${content.name}: ${localDuplicates.sorted()}"
        }

        if (content.entries.isNotEmpty()) {
            val entryNpcIds = content.entries.asSequence().map { it.npcId }.distinct().toSet()
            val declaredNpcIds = content.npcIds.toSet()
            if (entryNpcIds.intersect(declaredNpcIds).isEmpty()) {
                logger.warn(
                    "NpcContent {} entries ({}) do not overlap handled npcIds ({}).",
                    content.name,
                    entryNpcIds.sorted().joinToString(","),
                    declaredNpcIds.sorted().joinToString(","),
                )
            }
        }

        if (content.ownsSpawnDefinitions && content.entries.isEmpty()) {
            logger.warn("NpcContent {} owns spawn definitions but has no entries.", content.name)
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
