package net.dodian.uber.game.plugin

import net.dodian.uber.game.content.npc.NpcClickHandler
import net.dodian.uber.game.content.npc.NpcContentDefinition
import net.dodian.uber.game.content.npc.NpcSpawnDef
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import java.lang.reflect.Method

object NpcModuleDefinitionBuilder {
    @JvmStatic
    fun fromModule(module: Any, explicitName: String, ownsSpawnDefinitions: Boolean): NpcContentDefinition {
        val moduleClass = module::class.java
        val name = explicitName.ifBlank { moduleClass.simpleName }
        val npcIds = readNpcIds(moduleClass, module)
        val entries = readEntries(moduleClass, module)
        return NpcContentDefinition(
            name = name,
            npcIds = npcIds,
            ownsSpawnDefinitions = ownsSpawnDefinitions,
            entries = entries,
            onFirstClick = methodHandler(moduleClass, module, "onFirstClick"),
            onSecondClick = methodHandler(moduleClass, module, "onSecondClick"),
            onThirdClick = methodHandler(moduleClass, module, "onThirdClick"),
            onFourthClick = methodHandler(moduleClass, module, "onFourthClick"),
            onAttack = methodHandler(moduleClass, module, "onAttack"),
        )
    }

    private fun readNpcIds(moduleClass: Class<*>, module: Any): IntArray {
        val getter = moduleClass.methods.firstOrNull { it.name == "getNpcIds" && it.parameterCount == 0 }
            ?: error("NPC module ${moduleClass.name} is missing required npcIds property")
        return (getter.invoke(module) as? IntArray)
            ?: error("NPC module ${moduleClass.name} npcIds getter must return IntArray")
    }

    @Suppress("UNCHECKED_CAST")
    private fun readEntries(moduleClass: Class<*>, module: Any): List<NpcSpawnDef> {
        val getter = moduleClass.methods.firstOrNull { it.name == "getEntries" && it.parameterCount == 0 } ?: return emptyList()
        return (getter.invoke(module) as? List<NpcSpawnDef>).orEmpty()
    }

    private fun methodHandler(moduleClass: Class<*>, module: Any, methodName: String): NpcClickHandler {
        val method = resolveHandler(moduleClass, methodName) ?: return { _, _ -> false }
        return { client: Client, npc: Npc ->
            val result = method.invoke(module, client, npc)
            (result as? Boolean) == true
        }
    }

    private fun resolveHandler(moduleClass: Class<*>, methodName: String): Method? =
        moduleClass.methods.firstOrNull { method ->
            method.name == methodName &&
                method.parameterCount == 2 &&
                method.parameterTypes[0] == Client::class.java &&
                method.parameterTypes[1] == Npc::class.java
        }
}
