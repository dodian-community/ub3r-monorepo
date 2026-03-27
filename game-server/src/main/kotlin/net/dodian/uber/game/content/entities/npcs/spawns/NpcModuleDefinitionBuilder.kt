package net.dodian.uber.game.content.entities.npcs.spawns

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

object NpcModuleDefinitionBuilder {

    @JvmStatic
    fun fromModule(
        module: Any,
        explicitName: String = "",
        ownsSpawnDefinitions: Boolean = false,
    ): NpcContentDefinition {
        val moduleClass = module::class
        readDslDefinition(moduleClass, module, explicitName, ownsSpawnDefinitions)?.let { return it }
        val name = explicitName.ifBlank { moduleClass.simpleName ?: "NpcModule" }
        val entries = readEntries(moduleClass, module)
        val hasClickHandlers =
            findHandler(moduleClass, "onFirstClick") != null ||
                findHandler(moduleClass, "onSecondClick") != null ||
                findHandler(moduleClass, "onThirdClick") != null ||
                findHandler(moduleClass, "onFourthClick") != null ||
                findHandler(moduleClass, "onAttack") != null
        val npcIds =
            if (entries.isNotEmpty() && !hasClickHandlers) {
                intArrayOf()
            } else {
                mergeNpcIds(entries, readNpcIds(moduleClass, module))
            }
        return NpcContentDefinition(
            name = name,
            npcIds = npcIds,
            ownsSpawnDefinitions = ownsSpawnDefinitions,
            entries = entries,
            interactionSource = if (hasClickHandlers) NpcInteractionSource.LEGACY_REFLECTION else NpcInteractionSource.DSL,
            onFirstClick = readHandler(moduleClass, module, "onFirstClick"),
            onSecondClick = readHandler(moduleClass, module, "onSecondClick"),
            onThirdClick = readHandler(moduleClass, module, "onThirdClick"),
            onFourthClick = readHandler(moduleClass, module, "onFourthClick"),
            onAttack = readHandler(moduleClass, module, "onAttack"),
        )
    }

    private fun readDslDefinition(
        type: KClass<*>,
        module: Any,
        explicitName: String,
        ownsSpawnDefinitions: Boolean,
    ): NpcContentDefinition? {
        val property =
            type.memberProperties.firstOrNull { it.name == "definition" || it.name == "plugin" }
                ?: return null
        val value = property.getter.call(module) ?: return null
        return when (value) {
            is NpcPluginDefinition -> {
                val content = value.toContentDefinition(explicitName, ownsSpawnDefinitions)
                if (content.hasInteractionHandlers()) {
                    content.copy(npcIds = mergeNpcIds(content.entries, content.npcIds))
                } else {
                    content.copy(npcIds = intArrayOf())
                }
            }
            is NpcContentDefinition ->
                value
                    .copy(
                        name = explicitName.ifBlank { value.name },
                        npcIds =
                            if (value.hasInteractionHandlers()) {
                                mergeNpcIds(value.entries, value.npcIds)
                            } else {
                                intArrayOf()
                            },
                        ownsSpawnDefinitions = ownsSpawnDefinitions || value.ownsSpawnDefinitions,
                    )
            else -> null
        }
    }

    private fun mergeNpcIds(entries: List<NpcSpawnDef>, declaredNpcIds: IntArray?): IntArray {
        val merged = LinkedHashSet<Int>()
        declaredNpcIds?.forEach { merged += it }
        entries.forEach { merged += it.npcId }
        return merged.toIntArray()
    }

    private fun readNpcIds(type: KClass<*>, module: Any): IntArray? {
        val property = type.memberProperties.firstOrNull { it.name == "npcIds" } ?: return null
        val value = property.getter.call(module) ?: return null
        return when (value) {
            is IntArray -> value
            is Array<*> -> value.filterIsInstance<Int>().toIntArray()
            is Collection<*> -> value.filterIsInstance<Int>().toIntArray()
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readEntries(type: KClass<*>, module: Any): List<NpcSpawnDef> {
        val property = type.memberProperties.firstOrNull { it.name == "entries" } ?: return emptyList()
        val value = property.getter.call(module) ?: return emptyList()
        return (value as? List<*>)?.filterIsInstance<NpcSpawnDef>() ?: emptyList()
    }

    private fun readHandler(
        type: KClass<*>,
        module: Any,
        methodName: String,
    ): NpcClickHandler {
        val function = findHandler(type, methodName) ?: return NO_CLICK_HANDLER
        return { client: Client, npc: Npc ->
            val result = function.callBy(mapArguments(function, module, client, npc))
            result as? Boolean ?: false
        }
    }

    private fun findHandler(type: KClass<*>, methodName: String): KFunction<*>? {
        return type.memberFunctions.firstOrNull { function ->
            function.name == methodName &&
                function.parameters.count { it.kind == KParameter.Kind.VALUE } == 2
        }
    }

    private fun mapArguments(
        function: KFunction<*>,
        module: Any,
        client: Client,
        npc: Npc,
    ): Map<KParameter, Any> {
        val params = HashMap<KParameter, Any>(3)
        function.instanceParameter?.let { params[it] = module }
        function.parameters.forEach { parameter ->
            if (parameter.kind != KParameter.Kind.VALUE) return@forEach
            when (parameter.type.classifier) {
                Client::class -> params[parameter] = client
                Npc::class -> params[parameter] = npc
            }
        }
        return params
    }
}
