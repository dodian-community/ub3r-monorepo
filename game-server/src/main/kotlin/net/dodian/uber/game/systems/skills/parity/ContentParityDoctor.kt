package net.dodian.uber.game.systems.skills.parity

import net.dodian.uber.game.content.npcs.NO_CLICK_HANDLER
import net.dodian.uber.game.content.npcs.NpcContentDefinition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.systems.dispatch.ContentModuleIndex
import net.dodian.uber.game.systems.dispatch.npcs.NpcContentRegistry
import net.dodian.uber.game.systems.skills.plugin.SkillPluginRegistry
import net.dodian.uber.game.systems.skills.plugin.SkillPluginSnapshot

object ContentParityDoctor {
    fun scan(
        catalog: ContentParityCatalog = LegacyContentParityCatalog.default,
        skillSnapshot: SkillPluginSnapshot = SkillPluginRegistry.current(),
        npcLookup: (Int) -> NpcContentDefinition? = { npcId -> NpcContentRegistry.get(npcId) },
        discoveredSkills: Set<Skill> = ContentModuleIndex.skillPlugins.map { it.definition.skill }.toSet(),
    ): List<SkillDoctorFinding> {
        val findings = mutableListOf<SkillDoctorFinding>()

        catalog.requiredNpcClicks.forEach { key ->
            if (!npcClickRegistered(npcLookup, key.option, key.npcId)) {
                findings += SkillDoctorFinding(
                    code = "parity-missing-npc-route",
                    message = "Missing required NPC click route option=${key.option} npcId=${key.npcId}",
                )
            }
        }
        catalog.bannedNpcClicks.forEach { key ->
            if (npcClickRegistered(npcLookup, key.option, key.npcId)) {
                findings += SkillDoctorFinding(
                    code = "parity-banned-npc-route",
                    message = "Banned NPC click route is still registered option=${key.option} npcId=${key.npcId}",
                )
            }
        }

        catalog.requiredObjectClicks.forEach { key ->
            if (skillSnapshot.objectBinding(option = key.option, objectId = key.objectId) == null) {
                findings += SkillDoctorFinding(
                    code = "parity-missing-object-route",
                    message = "Missing required object click route option=${key.option} objectId=${key.objectId}",
                )
            }
        }
        catalog.bannedObjectClicks.forEach { key ->
            if (skillSnapshot.objectBinding(option = key.option, objectId = key.objectId) != null) {
                findings += SkillDoctorFinding(
                    code = "parity-banned-object-route",
                    message = "Banned object click route is still registered option=${key.option} objectId=${key.objectId}",
                )
            }
        }

        catalog.requiredItemOnItem.forEach { key ->
            if (skillSnapshot.itemOnItemBinding(key.leftItemId, key.rightItemId) == null) {
                findings += SkillDoctorFinding(
                    code = "parity-missing-item-on-item-route",
                    message = "Missing required item-on-item route left=${key.leftItemId} right=${key.rightItemId}",
                )
            }
        }
        catalog.bannedItemOnItem.forEach { key ->
            if (skillSnapshot.itemOnItemBinding(key.leftItemId, key.rightItemId) != null) {
                findings += SkillDoctorFinding(
                    code = "parity-banned-item-on-item-route",
                    message = "Banned item-on-item route is still registered left=${key.leftItemId} right=${key.rightItemId}",
                )
            }
        }

        val missingSkills = catalog.requiredSkillCoverage - discoveredSkills
        if (missingSkills.isNotEmpty()) {
            findings += SkillDoctorFinding(
                code = "parity-missing-skill-coverage",
                message = "Missing required skill plugin coverage: ${missingSkills.joinToString { it.name.lowercase() }}",
            )
        }

        val pluginsBySkill = ContentModuleIndex.skillPlugins.associateBy { it.definition.skill }
        catalog.requiredSkillRouteTypes.forEach { (skill, requiredTypes) ->
            val plugin = pluginsBySkill[skill]
            if (plugin == null) {
                return@forEach
            }
            requiredTypes.forEach { routeType ->
                val present =
                    when (routeType) {
                        SkillRouteType.OBJECT -> plugin.definition.objectBindings.isNotEmpty()
                        SkillRouteType.NPC -> plugin.definition.npcBindings.isNotEmpty()
                        SkillRouteType.ITEM_ON_ITEM -> plugin.definition.itemOnItemBindings.isNotEmpty()
                        SkillRouteType.BUTTON -> plugin.definition.buttonBindings.isNotEmpty()
                    }
                if (!present) {
                    findings += SkillDoctorFinding(
                        code = "parity-missing-skill-route-type",
                        message = "Skill ${skill.name.lowercase()} missing required route type $routeType",
                    )
                }
            }
        }

        return findings
    }

    private fun npcClickRegistered(
        npcLookup: (Int) -> NpcContentDefinition?,
        option: Int,
        npcId: Int,
    ): Boolean {
        val definition = npcLookup(npcId) ?: return false
        return when (option) {
            1 -> definition.onFirstClick !== NO_CLICK_HANDLER
            2 -> definition.onSecondClick !== NO_CLICK_HANDLER
            3 -> definition.onThirdClick !== NO_CLICK_HANDLER
            4 -> definition.onFourthClick !== NO_CLICK_HANDLER
            5 -> definition.onAttack !== NO_CLICK_HANDLER
            else -> false
        }
    }
}
