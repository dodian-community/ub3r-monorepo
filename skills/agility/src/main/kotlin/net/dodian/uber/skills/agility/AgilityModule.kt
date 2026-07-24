package net.dodian.uber.skills.agility

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

enum class AgilityMovementType { WALK, CLIMB }

data class AgilityObstacleDef(
    val course: String,
    val name: String,
    val objectId: Int,
    val requiredLevel: Int,
    val experience: Int,
    val movementType: AgilityMovementType,
    val startPosition: SkillPosition?,
    val deltaX: Int,
    val deltaY: Int,
    val destination: SkillPosition?,
    val durationMs: Int,
    val animationId: Int,
    val startAnimationId: Int?,
    val arrivalAnimationId: Int?,
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
        val start = obstacle.startPosition
        if (start != null && player.world.position != start) {
            return false
        }
        when (obstacle.movementType) {
            AgilityMovementType.WALK ->
                player.world.traverse(
                    deltaX = obstacle.deltaX,
                    deltaY = obstacle.deltaY,
                    durationMs = obstacle.durationMs,
                    movementAnimationId = obstacle.animationId,
                    startAnimationId = obstacle.startAnimationId,
                    arrivalAnimationId = obstacle.arrivalAnimationId,
                ) {
                    finishObstacle(player, obstacle)
                }
            AgilityMovementType.CLIMB ->
                player.world.climb(
                    destination = requireNotNull(obstacle.destination) { "climb obstacle ${obstacle.name} missing destination" },
                    animationId = obstacle.animationId,
                    delayMs = obstacle.durationMs,
                ) {
                    finishObstacle(player, obstacle)
                }
        }
        return true
    }

    private fun finishObstacle(player: net.dodian.uber.game.api.plugin.skills.SkillPlayer, obstacle: AgilityObstacleDef) {
        player.skills.gainXp(obstacle.experience, Skill.AGILITY)
        player.actions.triggerRandomEvent(obstacle.experience)
        player.ui.message("You successfully cross the ${obstacle.name.replace('_', ' ')}.")
    }

    private fun loadObstacles(): List<AgilityObstacleDef> =
        TomlRecordReader.readRecords("agility/courses.toml", "obstacle").map { row ->
            val movementType = AgilityMovementType.valueOf(row.getValue("movement_type").uppercase())
            val startPosition = row["start_x"]?.let { x ->
                SkillPosition(x.toInt(), row.getValue("start_y").toInt(), row["start_z"]?.toInt() ?: 0)
            }
            val destination = row["dest_x"]?.let { x ->
                SkillPosition(x.toInt(), row.getValue("dest_y").toInt(), row["dest_z"]?.toInt() ?: 0)
            }
            AgilityObstacleDef(
                course = row.getValue("course"),
                name = row.getValue("name"),
                objectId = row.getValue("object_id").toInt(),
                requiredLevel = row.getValue("required_level").toInt(),
                experience = row.getValue("experience").toInt(),
                movementType = movementType,
                startPosition = startPosition,
                deltaX = row["delta_x"]?.toInt() ?: 0,
                deltaY = row["delta_y"]?.toInt() ?: 0,
                destination = destination,
                durationMs = row.getValue("duration_ms").toInt(),
                animationId = row.getValue("animation_id").toInt(),
                startAnimationId = row["start_animation_id"]?.toInt(),
                arrivalAnimationId = row["arrival_animation_id"]?.toInt(),
            )
        }
}
