package net.dodian.uber.skills.agility

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class AgilityObstacleDef(
    val course: String,
    val name: String,
    val objectId: Int,
    val requiredLevel: Int,
    val experience: Int,
)

object AgilityModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.agility", "Agility")
    val obstacles: List<AgilityObstacleDef> by lazy { loadObstacles() }

    private val obstacleObjectIds: IntArray by lazy { obstacles.map { it.objectId }.toIntArray() }

    override val definition: SkillPluginDefinition = skillPlugin("Agility", Skill.AGILITY) {
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *obstacleObjectIds) { interaction ->
            crossObstacle(interaction)
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun crossObstacle(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val obstacle = obstacles.firstOrNull { it.objectId == interaction.target.id } ?: return false
        if (player.skills.current(Skill.AGILITY) < obstacle.requiredLevel) {
            player.ui.message("You need level ${obstacle.requiredLevel} agility to use this obstacle.")
            return true
        }
        player.skills.gainXp(obstacle.experience, Skill.AGILITY)
        player.actions.triggerRandomEvent(obstacle.experience)
        player.ui.message("You successfully cross the ${obstacle.name.replace('_', ' ')}.")
        return true
    }

    private fun loadObstacles(): List<AgilityObstacleDef> =
        TomlRecordReader.readRecords("agility/courses.toml", "obstacle").map { row ->
            AgilityObstacleDef(
                course = row.getValue("course"),
                name = row.getValue("name"),
                objectId = row.getValue("object_id").toInt(),
                requiredLevel = row.getValue("required_level").toInt(),
                experience = row.getValue("experience").toInt(),
            )
        }
}
