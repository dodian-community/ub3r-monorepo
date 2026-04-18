package net.dodian.uber.game.architecture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.streams.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventContractCoverageTest {
    private val sourceRoot: Path = Paths.get("src/main")
    private val eventsRoot: Path = sourceRoot.resolve("kotlin/net/dodian/uber/game/events")

    private data class SourceFileSnapshot(
        val path: Path,
        val normalized: String,
    )

    private val sourceFiles: List<SourceFileSnapshot> by lazy {
        Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .filter { it.extension == "kt" || it.extension == "java" }
                .map { file ->
                    SourceFileSnapshot(
                        path = file,
                        normalized = normalizeSource(Files.readString(file)),
                    )
                }
                .toList()
        }
    }

    private enum class Contract {
        STRICT,
        PRODUCER_ONLY,
        RESERVED_DEFINITION_ONLY,
    }

    private val strictEvents = setOf(
        "CommandEvent",
        "DialogueContinueEvent",
        "ItemDropEvent",
        "ItemExamineEvent",
        "NpcExamineEvent",
        "ObjectExamineEvent",
    )

    private val producerOnlyEvents = setOf(
        "ButtonClickEvent",
        "DialogueOptionEvent",
        "ItemClickEvent",
        "ItemOnItemEvent",
        "ItemOnNpcEvent",
        "ItemOnObjectEvent",
        "ItemOnPlayerEvent",
        "ItemPickupEvent",
        "LevelUpEvent",
        "MagicOnNpcEvent",
        "MagicOnObjectEvent",
        "MagicOnPlayerEvent",
        "NpcClickEvent",
        "NpcDeathEvent",
        "NpcDropEvent",
        "ObjectClickEvent",
        "PlayerAttackEvent",
        "PlayerDeathEvent",
        "PlayerLoginEvent",
        "PlayerLogoutEvent",
        "SkillActionCompleteEvent",
        "SkillActionInterruptEvent",
        "SkillActionStartEvent",
        "SkillProgressAppliedEvent",
        "SkillingActionCycleEvent",
        "SkillingActionStartedEvent",
        "SkillingActionStoppedEvent",
        "SkillingActionSucceededEvent",
        "TradeCancelEvent",
        "TradeCompleteEvent",
        "TradeRequestEvent",
        "ChatMessageEvent",
        "PrivateMessageEvent",
        "WalkEvent",
        "WorldTickEvent",
    )

    private val reservedDefinitionOnlyEvents = setOf(
        "PlayerTickEvent",
    )

    private val contracts: Map<String, Contract> by lazy {
        buildMap {
            strictEvents.forEach { put(it, Contract.STRICT) }
            producerOnlyEvents.forEach { put(it, Contract.PRODUCER_ONLY) }
            reservedDefinitionOnlyEvents.forEach { put(it, Contract.RESERVED_DEFINITION_ONLY) }
        }
    }

    @Test
    fun `every event type has exactly one definition and one contract entry`() {
        val definitions = eventDefinitionNames()

        val duplicateNames = definitions.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue(
            duplicateNames.isEmpty(),
            "Duplicate event class names found: $duplicateNames",
        )

        assertEquals(
            definitions.toSet(),
            contracts.keys,
            "Contract map must match defined event names exactly",
        )
    }

    @Test
    fun `event contract semantics hold`() {
        val failures = mutableListOf<String>()

        contracts.forEach { (eventName, contract) ->
            val producer = hasProducer(eventName)
            val subscriber = hasSubscriber(eventName)

            when (contract) {
                Contract.STRICT -> {
                    if (!producer || !subscriber) {
                        failures += "$eventName requires both producer and subscriber"
                    }
                }

                Contract.PRODUCER_ONLY -> {
                    if (!producer) {
                        failures += "$eventName requires a producer"
                    }
                }

                Contract.RESERVED_DEFINITION_ONLY -> {
                    if (producer || subscriber) {
                        failures += "$eventName is reserved but is currently wired"
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun `skill event catalog enumerates canonical skilling events with event suffix and payload fields`() {
        val catalogSource =
            Files.readString(
                Paths.get("src/main/kotlin/net/dodian/uber/game/events/skilling/SkillEventCatalog.kt"),
            )
        val eventsSource =
            Files.readString(
                Paths.get("src/main/kotlin/net/dodian/uber/game/events/skilling/SkillEvents.kt"),
            )

        assertTrue(catalogSource.contains("object SkillEventCatalog"))
        assertTrue(catalogSource.contains("val events: Set<KClass<out GameEvent>>"))
        assertTrue(catalogSource.contains("SkillActionStartEvent::class"))
        assertTrue(catalogSource.contains("SkillingActionStartedEvent::class"))

        val skillEventClassNames =
            Regex("""data class\s+([A-Za-z0-9_]+)\s*\(""")
                .findAll(eventsSource)
                .map { it.groupValues[1] }
                .toList()
        assertTrue(skillEventClassNames.isNotEmpty())
        assertTrue(skillEventClassNames.all { it.endsWith("Event") })

        assertTrue(eventsSource.contains("val client: Client"))
        assertTrue(eventsSource.contains("val actionName: String"))
    }

    private fun eventDefinitionNames(): List<String> {
        val names = mutableListOf<String>()
        Files.walk(eventsRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .forEach { file ->
                    Files.readAllLines(file).forEach { rawLine ->
                        val line = rawLine.trim()
                        if (line.startsWith("data class ")) {
                            Regex("""^data class\s+([A-Za-z0-9_]+Event)\s*\(""")
                                .find(line)
                                ?.groupValues
                                ?.getOrNull(1)
                                ?.let(names::add)
                        }
                    }
                }
        }
        return names
    }

    private fun hasProducer(eventName: String): Boolean {
        val eventPattern = Regex("""\b(?:new\s+)?${Regex.escape(eventName)}\s*\(""")
        for (snapshot in sourceFiles) {
            val content = snapshot.normalized
            if ((content.contains("GameEventBus.post(") ||
                    content.contains("GameEventBus.postWithResult(") ||
                    content.contains("GameEventBus.postAndReturn(") ||
                    content.contains("new $eventName(")) &&
                eventPattern.containsMatchIn(content)
            ) {
                return true
            }
        }
        return false
    }

    private fun hasSubscriber(eventName: String): Boolean {
        val reifiedOnPattern = Regex("""\bGameEventBus\.on<\s*$eventName\s*>""")
        val reifiedReturnablePattern = Regex("""\bGameEventBus\.onReturnable<\s*$eventName\s*>""")
        val classBasedOnPattern = Regex(
            """\bGameEventBus\.on\s*\(\s*$eventName::class\.java\s*,""",
        )
        val classBasedReturnablePattern = Regex(
            """\bGameEventBus\.onReturnable\s*\(\s*$eventName::class\.java\s*,""",
        )

        for (snapshot in sourceFiles) {
            val content = snapshot.normalized
            if (reifiedOnPattern.containsMatchIn(content) ||
                reifiedReturnablePattern.containsMatchIn(content) ||
                classBasedOnPattern.containsMatchIn(content) ||
                classBasedReturnablePattern.containsMatchIn(content)
            ) {
                return true
            }
        }
        return false
    }

    private fun normalizeSource(source: String): String {
        val withoutBlockComments = source.replace(Regex("""(?s)/\*.*?\*/"""), " ")
        val withoutLineComments = withoutBlockComments.replace(Regex("""(?m)//.*$"""), " ")
        return withoutLineComments.replace(Regex("""\s+"""), " ").trim()
    }
}
