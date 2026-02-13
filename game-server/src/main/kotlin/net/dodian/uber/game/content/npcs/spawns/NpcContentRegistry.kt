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
                name = "LegacySecondClickNpcContent",
                npcIds = LegacySecondClickNpcContent.npcIds,
                onSecondClick = LegacySecondClickNpcContent::onSecondClick,
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
