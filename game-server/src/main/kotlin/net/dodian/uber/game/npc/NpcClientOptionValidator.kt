package net.dodian.uber.game.npc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.api.plugin.skills.SkillNpcClickBinding
import net.dodian.uber.game.engine.systems.cache.CacheNpcDefinition
import net.dodian.uber.game.engine.systems.cache.CacheVarbitDefinition
import org.slf4j.LoggerFactory

object NpcClientOptionValidator {
    private val logger = LoggerFactory.getLogger(NpcClientOptionValidator::class.java)
    private val mapper = ObjectMapper()
        .registerKotlinModule()
        .enable(SerializationFeature.INDENT_OUTPUT)

    internal fun loadEffectiveClientOverrides(
        rawDefinitions: Map<Int, CacheNpcDefinition>,
    ): Map<Int, NpcEffectiveClientOverride> {
        val sourcePath = listOf(
            Path.of("game-client/src/main/java/com/osroyale/NpcDefinition.java"),
            Path.of("../game-client/src/main/java/com/osroyale/NpcDefinition.java"),
        ).firstOrNull(Files::isRegularFile) ?: run {
            logger.warn("Client NPC override source was not found; effective-client override diagnostics are unavailable.")
            return emptyMap()
        }
        return parseEffectiveClientOverrides(Files.readString(sourcePath), rawDefinitions)
    }

    internal fun parseEffectiveClientOverrides(
        source: String,
        rawDefinitions: Map<Int, CacheNpcDefinition>,
    ): Map<Int, NpcEffectiveClientOverride> {
        val uncommented = source.replace(Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL)), "")
        val casePattern = Regex("\\bcase\\s+(\\d+)\\s*:")
        val namePattern = Regex("entityDef\\.name\\s*=\\s*\"([^\"]*)\"")
        val actionPattern = Regex("entityDef\\.actions\\[(\\d+)]\\s*=\\s*\"([^\"]*)\"")
        val resetPattern = Regex("entityDef\\.actions\\s*=\\s*new\\s+String\\s*\\[5]")
        val overrides = linkedMapOf<Int, NpcEffectiveClientOverride>()
        val ids = arrayListOf<Int>()
        val actions = linkedMapOf<Int, String>()
        var name: String? = null
        var resetActions = false

        fun flush() {
            for (id in ids) {
                val raw = rawDefinitions[id]
                val effectiveActions = when {
                    resetActions -> arrayOfNulls<String>(5)
                    actions.isNotEmpty() -> raw?.actions?.copyOf() ?: arrayOfNulls(5)
                    else -> null
                }
                actions.forEach { (slot, action) ->
                    if (slot in 0..4) effectiveActions?.set(slot, action)
                }
                if (name != null || effectiveActions != null) {
                    overrides[id] = NpcEffectiveClientOverride(id, name, effectiveActions)
                }
            }
            ids.clear()
            actions.clear()
            name = null
            resetActions = false
        }

        for (line in uncommented.lineSequence()) {
            val cases = casePattern.findAll(line).map { it.groupValues[1].toInt() }.toList()
            if (cases.isNotEmpty()) ids += cases
            namePattern.find(line)?.let { name = it.groupValues[1] }
            if (resetPattern.containsMatchIn(line)) resetActions = true
            actionPattern.find(line)?.let { actions[it.groupValues[1].toInt()] = it.groupValues[2] }
            if (ids.isNotEmpty() && Regex("\\bbreak\\s*;").containsMatchIn(line)) flush()
        }
        flush()
        return overrides
    }

    fun validate(
        rawDefinitions: Map<Int, CacheNpcDefinition>,
        contents: Collection<NpcContentDefinition>,
        modules: Collection<NpcModule> = emptyList(),
        spawns: Collection<NpcSpawnDef> = emptyList(),
    ): NpcClientOptionValidationReport {
        val report = inspect(rawDefinitions, contents, modules, spawns)
        report.warnings.forEach(logger::warn)
        if (report.failures.isNotEmpty()) {
            error("NPC client option validation failed:\n${report.failures.joinToString("\n")}")
        }
        logger.info(
            "NPC client option validation passed: modules={} contentIds={} spawnIds={} interactiveModules={} checkedOptions={} warnings={}",
            report.moduleCount,
            report.contentIdsChecked,
            report.liveSpawnIdsChecked,
            report.interactiveModules,
            report.checkedOptions,
            report.warnings.size,
        )
        return report
    }

    internal fun inspect(
        rawDefinitions: Map<Int, CacheNpcDefinition>,
        contents: Collection<NpcContentDefinition>,
        modules: Collection<NpcModule> = emptyList(),
        spawns: Collection<NpcSpawnDef> = emptyList(),
        skillNpcBindings: Collection<SkillNpcClickBinding> = emptyList(),
        eventNpcOptions: Set<NpcOptionKey> = emptySet(),
        effectiveClientOverrides: Map<Int, NpcEffectiveClientOverride> = emptyMap(),
    ): NpcClientOptionValidationReport {
        val failures = ArrayList<String>()
        val warnings = ArrayList<String>()
        val identityMismatches = ArrayList<NpcIdentityMismatch>()
        val missingVisibleHandlers = ArrayList<NpcMissingVisibleHandler>()
        val effectiveClientOverrideConflicts = ArrayList<NpcEffectiveClientOverrideConflict>()
        val liveCapabilities = ArrayList<NpcLiveCapability>()
        var interactiveModules = 0
        var checkedOptions = 0
        var contentIdsChecked = 0
        val contentByIdentity = contents.associateBy { System.identityHashCode(it) }
        val contentDefinitions = if (contents.isNotEmpty()) contents else modules.map { it.definition }

        for (content in contentDefinitions) {
            for (npcId in content.npcIds.distinct().sorted()) {
                contentIdsChecked++
                val raw = rawDefinitions[npcId]
                if (raw == null) {
                    failures += "${content.name} npc=$npcId is registered in Kotlin content but has no raw client cache definition."
                    continue
                }

                val labels = handlerLabels(content)
                if (labels.isNotEmpty()) {
                    if (contentByIdentity.isEmpty() || contentByIdentity.containsKey(System.identityHashCode(content))) {
                        interactiveModules++
                    }
                    if (raw.name.isNotBlank() && !sameVisibleName(raw.name, content.name)) {
                        warnings += "NPC name mismatch: module=${content.name} npc=$npcId rawName='${raw.name}' handlers=${describeHandlers(content)}. Right-click uses the client/display id name."
                    }
                }

                for ((option, label) in labels) {
                    checkedOptions++
                    if (option == 5) {
                        if (raw.actions.none { it.equals("attack", ignoreCase = true) }) {
                            failures += mismatch(content, npcId, raw, option, label, "choose an NPC id whose client cache exposes Attack")
                        }
                        continue
                    }

                    val rawAction = raw.actions.getOrNull(option - 1)
                    if (rawAction.isNullOrBlank()) {
                        failures += mismatch(content, npcId, raw, option, label, "move this handler to a cache-visible option slot or choose a different NPC id/display id")
                    } else if (!sameVisibleName(rawAction, label)) {
                        warnings += "NPC action label mismatch: module=${content.name} npc=$npcId option=$option label='$label' rawAction='$rawAction'. Right-click text comes from the client cache."
                    }
                }
            }
        }

        val liveSpawnIds = spawns.asSequence()
            .filter { it.live }
            .map { it.npcId }
            .distinct()
            .sorted()
            .toList()
        for (npcId in liveSpawnIds) {
            if (!rawDefinitions.containsKey(npcId)) {
                failures += "Live Kotlin spawn npc=$npcId has no raw client cache definition."
            }
        }

        val liveSpawnCounts = spawns.asSequence()
            .filter { it.live }
            .groupingBy { it.npcId }
            .eachCount()
        val modulesByNpcId = modules
            .flatMap { module -> module.definition.npcIds.distinct().map { it to module } }
            .groupBy({ it.first }, { it.second })
        val contentOwners = contentDefinitions
            .flatMap { content ->
                handlerLabels(content).flatMap { (option, label) ->
                    content.npcIds.distinct().map { npcId -> NpcOptionKey(npcId, option) to "npc:${content.name}:$label" }
                }
            }
            .toMap()
        val skillOwners = skillNpcBindings
            .flatMap { binding ->
                binding.npcIds.distinct().map { npcId ->
                    NpcOptionKey(npcId, binding.option) to "skill"
                }
            }
            .toMap()

        for ((npcId, spawnCount) in liveSpawnCounts.toSortedMap()) {
            val raw = rawDefinitions[npcId] ?: continue
            val dispatchOwners = linkedMapOf<Int, String>()
            raw.actions.forEachIndexed { index, action ->
                if (action.isNullOrBlank()) return@forEachIndexed
                val key = NpcOptionKey(npcId, index + 1)
                val owner = contentOwners[key] ?: skillOwners[key] ?: if (key in eventNpcOptions) "event" else null
                if (owner != null) dispatchOwners[index + 1] = owner
            }
            val effective = effectiveClientOverrides[npcId]
            liveCapabilities += NpcLiveCapability(
                runtimeId = npcId,
                cacheName = raw.name,
                effectiveClientName = effective?.name ?: raw.name,
                visibleActions = (effective?.actions ?: raw.actions).toList(),
                dispatchOwners = dispatchOwners,
                liveSpawnCount = spawnCount,
            )
            for (module in modulesByNpcId[npcId].orEmpty().distinctBy { it.definition.name }) {
                if (!sameVisibleName(raw.name, module.definition.name)) {
                    identityMismatches += NpcIdentityMismatch(
                        npcId = npcId,
                        module = module.definition.name,
                        cacheName = raw.name,
                        liveSpawnCount = spawnCount,
                    )
                }
            }

            raw.actions.forEachIndexed { index, action ->
                val visibleAction = action?.trim().orEmpty()
                if (visibleAction.isEmpty() || visibleAction.equals("attack", ignoreCase = true)) return@forEachIndexed
                val key = NpcOptionKey(npcId, index + 1)
                val owner = contentOwners[key] ?: skillOwners[key] ?: if (key in eventNpcOptions) "event" else null
                if (owner == null) {
                    missingVisibleHandlers += NpcMissingVisibleHandler(
                        npcId = npcId,
                        cacheName = raw.name,
                        option = index + 1,
                        action = visibleAction,
                        liveSpawnCount = spawnCount,
                    )
                }
            }

            if (effective == null) continue
            val nameConflict = effective.name?.takeIf { it.isNotBlank() && !sameVisibleName(it, raw.name) }
            val actionConflict = effective.actions?.takeIf { !it.contentEquals(raw.actions) }
            if (nameConflict != null || actionConflict != null) {
                effectiveClientOverrideConflicts += NpcEffectiveClientOverrideConflict(
                    npcId = npcId,
                    cacheName = raw.name,
                    effectiveName = effective.name ?: raw.name,
                    cacheActions = raw.actions.toList(),
                    effectiveActions = effective.actions?.toList() ?: raw.actions.toList(),
                    liveSpawnCount = spawnCount,
                )
            }
        }

        val replacementCandidates = identityMismatches.map { mismatch ->
            val handlers = modulesByNpcId[mismatch.npcId]
                .orEmpty()
                .flatMap { handlerLabels(it.definition) }
                .distinct()
                .sortedBy { it.first }
            NpcReplacementAudit(
                npcId = mismatch.npcId,
                module = mismatch.module,
                cacheName = mismatch.cacheName,
                handlers = handlers.map { (option, label) -> NpcHandlerDump(option, label) },
                candidates = rawDefinitions.values
                    .asSequence()
                    .filter { candidate -> candidate.id != mismatch.npcId && sameVisibleName(candidate.name, mismatch.module) }
                    .map { candidate -> replacementCandidate(candidate, handlers) }
                    .sortedBy { it.npcId }
                    .toList(),
            )
        }

        return NpcClientOptionValidationReport(
            moduleCount = modules.size,
            contentIdsChecked = contentIdsChecked,
            liveSpawnIdsChecked = liveSpawnIds.size,
            interactiveModules = contentDefinitions.count { it.hasInteractionHandlers() },
            checkedOptions = checkedOptions,
            failures = failures,
            warnings = warnings,
            identityMismatches = identityMismatches,
            missingVisibleHandlers = missingVisibleHandlers,
            effectiveClientOverrideConflicts = effectiveClientOverrideConflicts,
            liveCapabilities = liveCapabilities,
            replacementCandidates = replacementCandidates,
        )
    }

    fun writeReports(
        rawDefinitions: Map<Int, CacheNpcDefinition>,
        resolvedDefinitions: Map<Int, CacheNpcDefinition>,
        contents: Collection<NpcContentDefinition>,
        modules: Collection<NpcModule>,
        spawns: Collection<NpcSpawnDef>,
        validation: NpcClientOptionValidationReport,
        varbits: Map<Int, CacheVarbitDefinition> = emptyMap(),
        reportsDir: Path = Path.of("build/reports"),
    ): NpcDiagnosticsReportPaths {
        Files.createDirectories(reportsDir)
        val cachePath = reportsDir.resolve("npc-cache-definitions.json")
        val runtimePath = reportsDir.resolve("npc-runtime-validation.json")
        val backlogPath = reportsDir.resolve("npc-interaction-migration-backlog.json")
        mapper.writeValue(cachePath.toFile(), rawDefinitions.values.sortedBy { it.id }.map { cacheDumpRow(it, varbits) })
        mapper.writeValue(
            runtimePath.toFile(),
            NpcRuntimeValidationDump(
                summary = NpcRuntimeValidationSummary(
                    rawCacheDefinitions = rawDefinitions.size,
                    modules = modules.size,
                    contentIds = contents.sumOf { it.npcIds.distinct().size },
                    liveSpawns = spawns.count { it.live },
                    uniqueLiveSpawnIds = spawns.asSequence().filter { it.live }.map { it.npcId }.distinct().count(),
                    failures = validation.failures.size,
                    warnings = validation.warnings.size,
                ),
                validation = validation,
                morphVariables = morphVariableDump(rawDefinitions, varbits),
                modules = modules.sortedBy { it.definition.name }.map { moduleDumpRow(it, rawDefinitions, resolvedDefinitions) },
                liveSpawnIds = spawns.asSequence()
                    .filter { it.live }
                    .groupingBy { it.npcId }
                    .eachCount()
                    .toSortedMap()
                    .map { (npcId, count) ->
                        NpcSpawnValidationDump(
                            npcId = npcId,
                            count = count,
                            rawCache = rawDefinitions[npcId]?.let(::cacheSummary),
                            missingRawCache = rawDefinitions[npcId] == null,
                        )
                    },
            ),
        )
        mapper.writeValue(
            backlogPath.toFile(),
            NpcInteractionMigrationBacklog(
                identityMismatches = validation.identityMismatches,
                missingVisibleHandlers = validation.missingVisibleHandlers,
                effectiveClientOverrideConflicts = validation.effectiveClientOverrideConflicts,
                replacementCandidates = validation.replacementCandidates,
            ),
        )
        logger.info("NPC diagnostics wrote {}, {}, and {}", cachePath, runtimePath, backlogPath)
        return NpcDiagnosticsReportPaths(cachePath, runtimePath, backlogPath)
    }

    private fun moduleDumpRow(
        module: NpcModule,
        rawDefinitions: Map<Int, CacheNpcDefinition>,
        resolvedDefinitions: Map<Int, CacheNpcDefinition>,
    ): NpcModuleValidationDump {
        val definition = module.definition
        return NpcModuleValidationDump(
            module = definition.name,
            primaryId = (module as? NpcFamily)?.primaryId,
            npcIds = definition.npcIds.distinct().sorted(),
            handlers = handlerLabels(definition).map { (option, label) -> NpcHandlerDump(option, label) },
            ids = definition.npcIds.distinct().sorted().map { npcId ->
                NpcIdValidationDump(
                    npcId = npcId,
                    rawCache = rawDefinitions[npcId]?.let(::cacheSummary),
                    resolvedCache = resolvedDefinitions[npcId]?.let(::cacheSummary),
                    cacheDiff = cacheDiff(rawDefinitions[npcId], resolvedDefinitions[npcId]),
                    missingRawCache = rawDefinitions[npcId] == null,
                )
            },
        )
    }

    private fun cacheDumpRow(
        definition: CacheNpcDefinition,
        varbits: Map<Int, CacheVarbitDefinition>,
    ): NpcCacheDefinitionDump =
        NpcCacheDefinitionDump(
            id = definition.id,
            name = definition.name,
            examine = definition.examine,
            size = definition.size,
            combatLevel = definition.combatLevel,
            standingAnimation = definition.standingAnimation,
            walkingAnimation = definition.walkingAnimation,
            halfTurnAnimation = definition.halfTurnAnimation,
            clockwiseTurnAnimation = definition.clockwiseTurnAnimation,
            anticlockwiseTurnAnimation = definition.anticlockwiseTurnAnimation,
            actions = definition.actions.toList(),
            transformVarbit = definition.transformVarbit,
            transformVarp = definition.transformVarp,
            transformChildren = definition.transformChildren,
            transformFallbackChild = definition.transformFallbackChild,
            varbit = definition.transformVarbit.takeIf { it >= 0 }?.let(varbits::get)?.let(::varbitDump),
        )

    private fun cacheSummary(definition: CacheNpcDefinition): NpcCacheSummary =
        NpcCacheSummary(
            name = definition.name,
            examine = definition.examine,
            size = definition.size,
            combatLevel = definition.combatLevel,
            actions = definition.actions.toList(),
            transformVarbit = definition.transformVarbit,
            transformVarp = definition.transformVarp,
            transformChildren = definition.transformChildren,
        )

    private fun varbitDump(definition: CacheVarbitDefinition): NpcVarbitDump =
        NpcVarbitDump(
            id = definition.id,
            varp = definition.varp,
            leastSignificantBit = definition.leastSignificantBit,
            mostSignificantBit = definition.mostSignificantBit,
            maxValue = definition.maxValue,
        )

    private fun morphVariableDump(
        rawDefinitions: Map<Int, CacheNpcDefinition>,
        varbits: Map<Int, CacheVarbitDefinition>,
    ): List<NpcMorphVariableDump> =
        rawDefinitions.values
            .filter { it.hasClientMorph() }
            .groupBy { definition ->
                if (definition.transformVarbit >= 0) {
                    "varbit:${definition.transformVarbit}"
                } else {
                    "varp:${definition.transformVarp}"
                }
            }
            .toSortedMap()
            .map { (key, definitions) ->
                val type = key.substringBefore(':')
                val id = key.substringAfter(':').toInt()
                NpcMorphVariableDump(
                    type = type,
                    id = id,
                    varbit = if (type == "varbit") varbits[id]?.let(::varbitDump) else null,
                    npcIds = definitions.map { it.id }.sorted(),
                    sharedByCount = definitions.size,
                )
            }

    private fun cacheDiff(raw: CacheNpcDefinition?, resolved: CacheNpcDefinition?): Map<String, CacheValueDiff> {
        if (raw == null || resolved == null) return emptyMap()
        val values = linkedMapOf<String, CacheValueDiff>()
        fun add(name: String, before: Any?, after: Any?) {
            if (before != after) values[name] = CacheValueDiff(before, after)
        }
        add("name", raw.name, resolved.name)
        add("examine", raw.examine, resolved.examine)
        add("size", raw.size, resolved.size)
        add("combatLevel", raw.combatLevel, resolved.combatLevel)
        add("standingAnimation", raw.standingAnimation, resolved.standingAnimation)
        add("walkingAnimation", raw.walkingAnimation, resolved.walkingAnimation)
        add("halfTurnAnimation", raw.halfTurnAnimation, resolved.halfTurnAnimation)
        add("clockwiseTurnAnimation", raw.clockwiseTurnAnimation, resolved.clockwiseTurnAnimation)
        add("anticlockwiseTurnAnimation", raw.anticlockwiseTurnAnimation, resolved.anticlockwiseTurnAnimation)
        add("actions", raw.actions.toList(), resolved.actions.toList())
        return values
    }

    private fun handlerLabels(content: NpcContentDefinition): List<Pair<Int, String>> {
        val labels = ArrayList<Pair<Int, String>>(5)
        if (content.onFirstClick !== NO_CLICK_HANDLER || content.onFirstClickCtx !== NO_CONTEXT_CLICK_HANDLER) labels += 1 to (content.optionLabel(1) ?: "first")
        if (content.onSecondClick !== NO_CLICK_HANDLER || content.onSecondClickCtx !== NO_CONTEXT_CLICK_HANDLER) labels += 2 to (content.optionLabel(2) ?: "second")
        if (content.onThirdClick !== NO_CLICK_HANDLER || content.onThirdClickCtx !== NO_CONTEXT_CLICK_HANDLER) labels += 3 to (content.optionLabel(3) ?: "third")
        if (content.onFourthClick !== NO_CLICK_HANDLER || content.onFourthClickCtx !== NO_CONTEXT_CLICK_HANDLER) labels += 4 to (content.optionLabel(4) ?: "fourth")
        if (content.onAttack !== NO_CLICK_HANDLER || content.onAttackCtx !== NO_CONTEXT_CLICK_HANDLER) labels += 5 to (content.optionLabel(5) ?: "attack")
        return labels
    }

    private fun replacementCandidate(
        candidate: CacheNpcDefinition,
        handlers: List<Pair<Int, String>>,
    ): NpcReplacementCandidate {
        val visibleActions = candidate.actions.mapIndexedNotNull { index, action ->
            action?.trim()?.takeIf(String::isNotEmpty)?.let { index + 1 to it }
        }
        val missingHandlers = handlers.filter { (option, label) ->
            if (option == 5) visibleActions.none { (_, action) -> action.equals("attack", ignoreCase = true) }
            else visibleActions.none { (candidateOption, action) -> candidateOption == option && sameVisibleName(action, label) }
        }
        val unownedActions = visibleActions.filter { (option, action) ->
            !action.equals("attack", ignoreCase = true) && handlers.none { (handlerOption, label) ->
                handlerOption == option && sameVisibleName(action, label)
            }
        }
        val reasons = buildList {
            if (missingHandlers.isNotEmpty()) add("missing required handlers: ${missingHandlers.joinToString { "${it.first}:${it.second}" }}")
            if (unownedActions.isNotEmpty()) add("adds unowned visible actions: ${unownedActions.joinToString { "${it.first}:${it.second}" }}")
        }
        return NpcReplacementCandidate(
            npcId = candidate.id,
            cacheName = candidate.name,
            visibleActions = visibleActions.map { (option, action) -> NpcHandlerDump(option, action) },
            eligible = reasons.isEmpty(),
            reasons = reasons,
        )
    }

    private fun describeHandlers(content: NpcContentDefinition): String =
        handlerLabels(content).joinToString("|") { (option, label) -> "$option:$label" }

    private fun mismatch(
        content: NpcContentDefinition,
        npcId: Int,
        raw: CacheNpcDefinition,
        option: Int,
        label: String,
        hint: String,
    ): String =
        "module=${content.name} npc=$npcId rawName='${raw.name}' registers option=$option label='$label' " +
            "but raw cache actions=${raw.actions.contentToString()}. Hint: $hint."

    private fun sameVisibleName(raw: String, module: String): Boolean =
        normalizeName(raw) == normalizeName(module)

    private fun normalizeName(value: String): String =
        value.replace('_', ' ').trim().lowercase()
}

data class NpcClientOptionValidationReport(
    val moduleCount: Int,
    val contentIdsChecked: Int,
    val liveSpawnIdsChecked: Int,
    val interactiveModules: Int,
    val checkedOptions: Int,
    val failures: List<String>,
    val warnings: List<String>,
    val identityMismatches: List<NpcIdentityMismatch> = emptyList(),
    val missingVisibleHandlers: List<NpcMissingVisibleHandler> = emptyList(),
    val effectiveClientOverrideConflicts: List<NpcEffectiveClientOverrideConflict> = emptyList(),
    val liveCapabilities: List<NpcLiveCapability> = emptyList(),
    val replacementCandidates: List<NpcReplacementAudit> = emptyList(),
) {
    val optionViolations: List<String> get() = failures
    val nameWarnings: List<String> get() = warnings
}

data class NpcDiagnosticsReportPaths(
    val cacheDefinitions: Path,
    val runtimeValidation: Path,
    val migrationBacklog: Path,
)

data class NpcOptionKey(val npcId: Int, val option: Int)

data class NpcEffectiveClientOverride(
    val id: Int,
    val name: String? = null,
    val actions: Array<String?>? = null,
)

data class NpcIdentityMismatch(
    val npcId: Int,
    val module: String,
    val cacheName: String,
    val liveSpawnCount: Int,
)

data class NpcMissingVisibleHandler(
    val npcId: Int,
    val cacheName: String,
    val option: Int,
    val action: String,
    val liveSpawnCount: Int,
)

data class NpcEffectiveClientOverrideConflict(
    val npcId: Int,
    val cacheName: String,
    val effectiveName: String,
    val cacheActions: List<String?>,
    val effectiveActions: List<String?>,
    val liveSpawnCount: Int,
)

data class NpcInteractionMigrationBacklog(
    val identityMismatches: List<NpcIdentityMismatch>,
    val missingVisibleHandlers: List<NpcMissingVisibleHandler>,
    val effectiveClientOverrideConflicts: List<NpcEffectiveClientOverrideConflict>,
    val replacementCandidates: List<NpcReplacementAudit> = emptyList(),
)

data class NpcReplacementAudit(
    val npcId: Int,
    val module: String,
    val cacheName: String,
    val handlers: List<NpcHandlerDump>,
    val candidates: List<NpcReplacementCandidate>,
)

data class NpcReplacementCandidate(
    val npcId: Int,
    val cacheName: String,
    val visibleActions: List<NpcHandlerDump>,
    val eligible: Boolean,
    val reasons: List<String>,
)

data class NpcLiveCapability(
    val runtimeId: Int,
    val cacheName: String,
    val effectiveClientName: String,
    val visibleActions: List<String?>,
    val dispatchOwners: Map<Int, String>,
    val liveSpawnCount: Int,
)

data class NpcCacheDefinitionDump(
    val id: Int,
    val name: String,
    val examine: String,
    val size: Int,
    val combatLevel: Int,
    val standingAnimation: Int,
    val walkingAnimation: Int,
    val halfTurnAnimation: Int,
    val clockwiseTurnAnimation: Int,
    val anticlockwiseTurnAnimation: Int,
    val actions: List<String?>,
    val transformVarbit: Int,
    val transformVarp: Int,
    val transformChildren: List<Int>,
    val transformFallbackChild: Int,
    val varbit: NpcVarbitDump?,
)

data class NpcRuntimeValidationDump(
    val summary: NpcRuntimeValidationSummary,
    val validation: NpcClientOptionValidationReport,
    val morphVariables: List<NpcMorphVariableDump>,
    val modules: List<NpcModuleValidationDump>,
    val liveSpawnIds: List<NpcSpawnValidationDump>,
)

data class NpcRuntimeValidationSummary(
    val rawCacheDefinitions: Int,
    val modules: Int,
    val contentIds: Int,
    val liveSpawns: Int,
    val uniqueLiveSpawnIds: Int,
    val failures: Int,
    val warnings: Int,
)

data class NpcModuleValidationDump(
    val module: String,
    val primaryId: Int?,
    val npcIds: List<Int>,
    val handlers: List<NpcHandlerDump>,
    val ids: List<NpcIdValidationDump>,
)

data class NpcHandlerDump(
    val option: Int,
    val label: String,
)

data class NpcIdValidationDump(
    val npcId: Int,
    val rawCache: NpcCacheSummary?,
    val resolvedCache: NpcCacheSummary?,
    val cacheDiff: Map<String, CacheValueDiff>,
    val missingRawCache: Boolean,
)

data class NpcSpawnValidationDump(
    val npcId: Int,
    val count: Int,
    val rawCache: NpcCacheSummary?,
    val missingRawCache: Boolean,
)

data class NpcCacheSummary(
    val name: String,
    val examine: String,
    val size: Int,
    val combatLevel: Int,
    val actions: List<String?>,
    val transformVarbit: Int,
    val transformVarp: Int,
    val transformChildren: List<Int>,
)

data class NpcVarbitDump(
    val id: Int,
    val varp: Int,
    val leastSignificantBit: Int,
    val mostSignificantBit: Int,
    val maxValue: Int,
)

data class NpcMorphVariableDump(
    val type: String,
    val id: Int,
    val varbit: NpcVarbitDump?,
    val npcIds: List<Int>,
    val sharedByCount: Int,
)

data class CacheValueDiff(
    val raw: Any?,
    val resolved: Any?,
)
