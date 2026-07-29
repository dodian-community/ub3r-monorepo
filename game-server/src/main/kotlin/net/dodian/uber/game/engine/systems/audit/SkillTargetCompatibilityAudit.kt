package net.dodian.uber.game.engine.systems.audit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.engine.config.ServerPaths
import net.dodian.uber.skills.agility.AgilityModule
import net.dodian.uber.skills.cooking.CookingModule
import net.dodian.uber.skills.crafting.CraftingModule
import net.dodian.uber.skills.farming.FarmingModule
import net.dodian.uber.skills.firemaking.FiremakingModule
import net.dodian.uber.skills.fishing.FishingModule
import net.dodian.uber.skills.fletching.FletchingModule
import net.dodian.uber.skills.herblore.HerbloreModule
import net.dodian.uber.skills.mining.MiningModule
import net.dodian.uber.skills.prayer.PrayerModule
import net.dodian.uber.skills.runecrafting.RunecraftingModule
import net.dodian.uber.skills.slayer.SlayerModule
import net.dodian.uber.skills.smithing.SmithingModule
import net.dodian.uber.skills.thieving.ThievingModule
import net.dodian.uber.skills.woodcutting.WoodcuttingModule
import net.dodian.uber.skills.skillguide.SkillguideModule
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

/**
 * Developer-only compatibility inventory for the client revision used by this server.
 *
 * This deliberately reads the committed RSCM files rather than runtime cache definitions:
 * it verifies content routes without making the server depend on developer mappings.  Tarnish
 * is an optional external reference; it is never required for normal builds or test execution.
 */
object SkillTargetCompatibilityAudit {
    enum class TargetKind { OBJECT, NPC }
    /**
     * Tarnish is evidence for existing content only.  The audit never labels an unregistered
     * Tarnish target as a route to add.
     */
    enum class Status { EXISTING_CONTENT_REMAP, EXISTING_CONTENT_VERIFIED, TARNISH_ONLY, AMBIGUOUS }

    data class TargetBinding(
        val plugin: String,
        val kind: TargetKind,
        val id: Int,
        val option: Int?,
        val route: String,
    )

    data class LocalDefinition(val id: Int, val name: String?, val options: List<String>)

    data class AuditEntry(
        val binding: TargetBinding,
        val local: LocalDefinition?,
        val status: Status,
        val tarnishCandidates: List<Int>,
        val evidence: String,
    )

    data class InterfaceBinding(val owner: String, val kind: String, val id: Int, val excluded: Boolean = false)

    data class ExistingContentRemap(
        val plugin: String,
        val kind: TargetKind,
        val legacyId: Int,
        val currentId: Int,
        val option: Int,
        val route: String,
        val evidence: String,
    )

    /**
     * Every accepted remap must replace a route that already existed in this server.  This is
     * intentionally small and reviewable; Tarnish discoveries are not auto-promoted into it.
     */
    val acceptedExistingContentRemaps = listOf(
        ExistingContentRemap("Fishing", TargetKind.NPC, 1514, 1518, 1, "npc-click", "Small Net/Bait route; local action and current Fishing behavior are unchanged."),
        ExistingContentRemap("Fishing", TargetKind.NPC, 1506, 1526, 1, "npc-click", "Lure/Bait route; local action and current Fishing behavior are unchanged."),
        ExistingContentRemap("Fishing", TargetKind.NPC, 1510, 1519, 1, "npc-click", "Cage/Harpoon primary route; local action and current Fishing behavior are unchanged."),
        ExistingContentRemap("Fishing", TargetKind.NPC, 1510, 1519, 3, "npc-click", "Cage/Harpoon alternate route; local action and current Fishing behavior are unchanged."),
    )

    private val plugins: List<SkillPlugin> = listOf(
        AgilityModule, CookingModule, CraftingModule, FarmingModule, FiremakingModule,
        FishingModule, FletchingModule, HerbloreModule, MiningModule, PrayerModule,
        RunecraftingModule, SlayerModule, SmithingModule, ThievingModule, WoodcuttingModule,
    )

    /** All route registrations, including item-on-object and magic-on-object routes. */
    @JvmStatic
    fun targetBindings(): List<TargetBinding> = plugins.flatMap { plugin ->
        val definition = plugin.definition
        buildList {
            definition.objectBindings.forEach { binding ->
                binding.objectIds.forEach { add(TargetBinding(definition.name, TargetKind.OBJECT, it, binding.option, "object-click")) }
            }
            definition.itemOnObjectBindings.forEach { binding ->
                binding.objectIds.forEach { add(TargetBinding(definition.name, TargetKind.OBJECT, it, null, "item-on-object")) }
            }
            definition.magicOnObjectBindings.forEach { binding ->
                binding.objectIds.forEach { add(TargetBinding(definition.name, TargetKind.OBJECT, it, null, "magic-on-object")) }
            }
            definition.npcBindings.forEach { binding ->
                binding.npcIds.forEach { add(TargetBinding(definition.name, TargetKind.NPC, it, binding.option, "npc-click")) }
            }
        }
    }.distinct().sortedWith(compareBy(TargetBinding::plugin, TargetBinding::kind, TargetBinding::id, TargetBinding::route, TargetBinding::option))

    /** Standard server-owned UI bindings.  The custom make-all presentation has no entry here. */
    @JvmStatic
    fun interfaceBindings(): List<InterfaceBinding> = buildList {
        plugins.forEach { plugin ->
            plugin.definition.buttonBindings.forEach { binding ->
                add(InterfaceBinding(plugin.definition.name, "button", binding.requiredInterfaceId))
                binding.rawButtonIds.forEach { add(InterfaceBinding(plugin.definition.name, "button-component", it)) }
            }
            plugin.definition.itemGridBindings.forEach { add(InterfaceBinding(plugin.definition.name, "item-grid", it.interfaceId)) }
        }
        SkillguideModule.definition.buttonBindings.forEach { binding ->
            add(InterfaceBinding("Skillguide", "button", binding.requiredInterfaceId))
            binding.rawButtonIds.forEach { add(InterfaceBinding("Skillguide", "button-component", it)) }
        }
        // The furnace is an engine bridge, not a plugin route, and must remain visible to this audit.
        add(InterfaceBinding("SmithingSmeltingBridge", "chatbox-interface", 2400))
    }.filter { it.id >= 0 }.distinct().sortedWith(compareBy(InterfaceBinding::owner, InterfaceBinding::kind, InterfaceBinding::id))

    @JvmStatic
    fun localDefinitions(kind: TargetKind, root: Path = ServerPaths.definition("mappings", "cache-rev218")): Map<Int, LocalDefinition> =
        parseRscm(root.resolve(if (kind == TargetKind.OBJECT) "loc.rscm" else "npc.rscm"), if (kind == TargetKind.OBJECT) "loc" else "npc")

    @JvmStatic
    fun audit(tarnishRoot: Path? = null): List<AuditEntry> {
        val objects = localDefinitions(TargetKind.OBJECT)
        val npcs = localDefinitions(TargetKind.NPC)
        val tarnish = tarnishRoot?.takeIf { it.isDirectory() }?.let(::loadTarnish)
        return targetBindings().map { binding ->
            val local = (if (binding.kind == TargetKind.OBJECT) objects else npcs)[binding.id]
            classify(binding, local, tarnish)
        }
    }

    @JvmStatic
    fun renderMarkdown(entries: List<AuditEntry>, interfaces: List<InterfaceBinding> = interfaceBindings(), tarnishRoot: Path? = null): String = buildString {
        appendLine("# Skills object/NPC compatibility audit")
        appendLine()
        appendLine("Generated from active skill-plugin route registrations and the committed rev218 RSCM mappings.")
        appendLine("Tarnish reference: ${tarnishRoot?.toAbsolutePath() ?: "not supplied"}.")
        appendLine("Item IDs and the custom make-all presentation are intentionally out of scope.")
        appendLine()
        appendLine("## Object and NPC bindings")
        appendLine()
        appendLine("| Plugin | Route | Target | Local definition | Status | Tarnish candidates | Evidence |")
        appendLine("|---|---|---:|---|---|---|---|")
        entries.forEach { entry ->
            val local = entry.local?.let { definition ->
                listOfNotNull(definition.name, definition.options.takeIf { it.isNotEmpty() }?.joinToString(" / ")).joinToString(" — ")
            } ?: "missing local definition"
            appendLine("| ${entry.binding.plugin} | ${entry.binding.route}${entry.binding.option?.let { " option $it" } ?: ""} | ${entry.binding.kind.name.lowercase()} ${entry.binding.id} | ${escape(local)} | ${entry.status.name.lowercase().replace('_', ' ')} | ${entry.tarnishCandidates.joinToString().ifBlank { "—" }} | ${escape(entry.evidence)} |")
        }
        appendLine()
        appendLine("## Standard interface bindings")
        appendLine()
        appendLine("These are server routes/components for review against Tarnish source. They are not treated as item IDs.")
        appendLine()
        appendLine("| Owner | Binding | ID | Status |")
        appendLine("|---|---|---:|---|")
        interfaces.forEach { binding -> appendLine("| ${binding.owner} | ${binding.kind} | ${binding.id} | ${if (binding.excluded) "excluded custom UI" else "review against Tarnish handler/interface"} |") }
    }

    private fun classify(binding: TargetBinding, local: LocalDefinition?, tarnish: TarnishReference?): AuditEntry {
        if (local == null) return AuditEntry(binding, null, Status.AMBIGUOUS, emptyList(), "No committed rev218 ${binding.kind.name.lowercase()} mapping exists.")
        acceptedExistingContentRemaps.firstOrNull {
            it.plugin == binding.plugin && it.kind == binding.kind && it.currentId == binding.id && it.option == binding.option && it.route == binding.route
        }?.let { remap ->
            return AuditEntry(binding, local, Status.EXISTING_CONTENT_REMAP, listOf(remap.legacyId), remap.evidence)
        }
        if (tarnish == null) return AuditEntry(binding, local, Status.AMBIGUOUS, emptyList(), "Local definition resolved; Tarnish reference was not supplied.")
        val names = if (binding.kind == TargetKind.OBJECT) tarnish.objects else tarnish.npcs
        if (binding.id in tarnish.handlerIdsByPlugin[binding.plugin].orEmpty()) {
            return AuditEntry(binding, local, Status.EXISTING_CONTENT_VERIFIED, listOf(binding.id), "Exact ID is referenced by Tarnish's corresponding skill implementation.")
        }
        val sameId = names[binding.id]
        if (sameId != null && sameId == normalize(local.name)) {
            return AuditEntry(binding, local, Status.EXISTING_CONTENT_VERIFIED, listOf(binding.id), "Same ID and normalized name in Tarnish reference.")
        }
        val candidates = names.filterValues { it == normalize(local.name) }.keys.sorted()
        val candidateIsAlreadyBound = candidates.any { candidate ->
            targetBindings().any { existing ->
                existing.plugin == binding.plugin && existing.kind == binding.kind && existing.id == candidate
            }
        }
        if (candidateIsAlreadyBound) {
            return AuditEntry(
                binding,
                local,
                Status.AMBIGUOUS,
                candidates,
                "A same-name Tarnish target is already registered as a separate local route; it is not an existing-content remap.",
            )
        }
        return when (candidates.size) {
            1 -> AuditEntry(binding, local, Status.TARNISH_ONLY, candidates, "A Tarnish-only name match is reported but never registered as new local content.")
            0 -> AuditEntry(binding, local, Status.AMBIGUOUS, emptyList(), "No matching Tarnish definition/spawn name.")
            else -> AuditEntry(binding, local, Status.AMBIGUOUS, candidates, "Same-name variants require location, type, orientation, and click-option review.")
        }
    }

    private data class TarnishReference(
        val objects: Map<Int, String>,
        val npcs: Map<Int, String>,
        val handlerIdsByPlugin: Map<String, Set<Int>>,
    )

    private fun loadTarnish(root: Path): TarnishReference {
        val mapper = ObjectMapper()
        fun records(file: Path): List<JsonNode> = if (Files.isRegularFile(file)) mapper.readTree(file.toFile()).toList() else emptyList()
        val objects = records(root.resolve("data/def/object/global_objects.json"))
            .mapNotNull { node -> node["id"]?.asInt()?.let { id -> id to normalize(node["name"]?.asText()) } }.toMap()
        val npcs = records(root.resolve("data/def/npc/npc_definitions.json"))
            .mapNotNull { node -> node["id"]?.asInt()?.let { id -> id to normalize(node["name"]?.asText()) } }.toMap()
        val skillDirectories = mapOf(
            "Agility" to "agility", "Cooking" to "cooking", "Crafting" to "crafting", "Farming" to "farming",
            "Fishing" to "fishing", "Mining" to "mining", "Prayer" to "prayer", "Runecrafting" to "runecrafting",
            "Smithing" to "smithing", "Thieving" to "thieving", "Woodcutting" to "woodcutting",
        )
        val handlerIds = skillDirectories.mapValues { (_, directory) ->
            val source = root.resolve("src/main/java/com/osroyale/content/skill/impl/$directory")
            if (!Files.isDirectory(source)) emptySet() else buildSet {
                Files.walk(source).use { paths ->
                    paths.filter { it.toString().endsWith(".java") }.forEach { path ->
                        Regex("\\b\\d+\\b").findAll(Files.readString(path)).forEach { add(it.value.toInt()) }
                    }
                }
            }
        }
        return TarnishReference(objects, npcs, handlerIds)
    }

    private fun parseRscm(file: Path, namespace: String): Map<Int, LocalDefinition> {
        require(Files.isRegularFile(file)) { "Missing committed mapping: $file" }
        val sections = linkedMapOf<Int, MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        Files.readAllLines(file).forEach { line ->
            val header = Regex("^\\[${namespace}_(\\d+)]$").matchEntire(line)
            if (header != null) {
                current = linkedMapOf<String, String>().also { sections[header.groupValues[1].toInt()] = it }
            } else if (current != null && '=' in line) {
                val (key, value) = line.split('=', limit = 2)
                current!![key] = value
            }
        }
        return sections.mapValues { (id, fields) ->
            LocalDefinition(id, fields["name"], (1..5).mapNotNull { fields["op$it"] }.filterNot { it == "hidden" })
        }
    }

    private fun normalize(value: String?): String = value.orEmpty().lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    private fun escape(value: String): String = value.replace("|", "\\|").replace("\n", " ")
}

/** `./gradlew :game-server:runSkillTargetAudit -PtarnishRoot=/absolute/path/to/tarnish-main/game-server` */
object SkillTargetCompatibilityAuditMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val root = args.firstOrNull()?.let(Path::of)
        print(SkillTargetCompatibilityAudit.renderMarkdown(SkillTargetCompatibilityAudit.audit(root), tarnishRoot = root))
    }
}
