package net.dodian.uber.skills.agility

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

enum class AgilityMovementType { WALK, CLIMB, RUN }

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
    val requiredStage: Int?,
    val nextStage: Int?,
    val isLapCompletion: Boolean,
    val ticketBase: Int,
    val ticketScaleDivisor: Int,
    val bonusExperience: Int,
    val lapMessage: String?,
    val npcText: String?,
)

object AgilityModule : SkillPlugin {
    const val AGILITY_TICKET_ID = 2996

    val descriptor = SkillModuleDescriptor("skill.agility", "Agility")
    val obstacles: List<AgilityObstacleDef> by lazy { loadObstacles() }

    private val obstacleObjectIds: IntArray by lazy { obstacles.map { it.objectId }.distinct().toIntArray() }
    private val werewolfEntryGateId = 11636

    override val definition: SkillPluginDefinition = skillPlugin("Agility", Skill.AGILITY) {
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *obstacleObjectIds) { interaction ->
            crossObstacle(interaction)
        }
        objectClick(preset = PolicyPreset.DIALOGUE, option = 1, werewolfEntryGateId) { interaction ->
            enterWerewolfCourse(interaction)
        }
        itemClick(preset = PolicyPreset.DIALOGUE, option = 1, AGILITY_TICKET_ID) { interaction ->
            exchangeTickets(interaction.player)
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
        val matchingObstacles = obstacles.filter { it.objectId == interaction.target.id }
        if (matchingObstacles.isEmpty()) return false

        // Match by startPosition if multiple obstacles share objectId (e.g. crumbling walls or pipe entry tiles)
        val obstacle = matchingObstacles.firstOrNull { candidate ->
            candidate.startPosition == null || player.world.position == candidate.startPosition
        } ?: matchingObstacles.first()

        if (player.skills.current(Skill.AGILITY) < obstacle.requiredLevel) {
            player.ui.message("You need level ${obstacle.requiredLevel} agility to use this obstacle.")
            return true
        }

        val start = obstacle.startPosition
        if (start != null && player.world.position != start) {
            return false
        }

        obstacle.npcText?.let { text ->
            player.ui.message(text)
        }

        when (obstacle.movementType) {
            AgilityMovementType.WALK, AgilityMovementType.RUN ->
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

    private fun enterWerewolfCourse(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.AGILITY) < 60) {
            player.ui.npcDialogue(dialogueId = 616, npcId = 5928)
            return true
        }
        player.world.replaceObject(interaction.target, replacementId = werewolfEntryGateId, restoreTicks = 10, type = 2)
        player.ui.npcDialogue(dialogueId = 601, npcId = 5928)
        player.world.teleport(SkillPosition(3549, 9865, 0))
        return true
    }

    private fun finishObstacle(player: SkillPlayer, obstacle: AgilityObstacleDef) {
        player.skills.gainXp(obstacle.experience, Skill.AGILITY)
        player.actions.triggerRandomEvent(obstacle.experience)
        player.ui.message("You successfully cross the ${obstacle.name.replace('_', ' ')}.")

        val stageKey = "stage_${obstacle.course}"
        val currentStage = player.moduleState.get(descriptor.id, stageKey)?.toIntOrNull() ?: 0

        if (obstacle.requiredStage != null) {
            val next = if (currentStage >= obstacle.requiredStage) (obstacle.nextStage ?: 0) else currentStage
            player.moduleState.put(descriptor.id, stageKey, next.toString())
        }

        if (obstacle.isLapCompletion && currentStage >= (obstacle.requiredStage ?: 0)) {
            player.moduleState.put(descriptor.id, stageKey, "0")

            if (obstacle.bonusExperience > 0) {
                player.skills.gainXp(obstacle.bonusExperience, Skill.AGILITY)
            }

            if (obstacle.ticketBase > 0) {
                val agilityLevel = player.skills.current(Skill.AGILITY)
                val maxExtra = if (obstacle.ticketScaleDivisor > 0) (agilityLevel / obstacle.ticketScaleDivisor) else 0
                val extra = if (maxExtra > 0) player.random.between(0, maxExtra) else 0
                val tickets = obstacle.ticketBase + extra
                player.inventory.add(AGILITY_TICKET_ID, tickets)
            }

            obstacle.lapMessage?.let { msg ->
                player.ui.message(msg)
            }
        }
    }

    private fun exchangeTickets(player: SkillPlayer): Boolean {
        val amount = player.inventory.amount(AGILITY_TICKET_ID)
        if (amount < 10) {
            player.ui.message("You must hand in at least 10 tickets at once.")
            return true
        }
        if (player.inventory.transaction { remove(AGILITY_TICKET_ID, amount) }) {
            val xp = amount * 700
            player.skills.gainXp(xp, Skill.AGILITY)
            player.ui.message("You exchange your $amount agility tickets.")
        }
        return true
    }

    private fun loadObstacles(): List<AgilityObstacleDef> {
        val courseFiles = listOf(
            "agility/courses/gnome.toml",
            "agility/courses/barbarian.toml",
            "agility/courses/wilderness.toml",
            "agility/courses/werewolf.toml",
            "agility/courses/shortcuts.toml",
        )
        return courseFiles.flatMap { file ->
            TomlRecordReader.readRecords(file, "obstacle").map { row ->
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
                    requiredStage = row["required_stage"]?.toInt(),
                    nextStage = row["next_stage"]?.toInt(),
                    isLapCompletion = row["is_lap_completion"]?.toBoolean() ?: false,
                    ticketBase = row["ticket_base"]?.toInt() ?: 0,
                    ticketScaleDivisor = row["ticket_scale_divisor"]?.toInt() ?: 0,
                    bonusExperience = row["bonus_experience"]?.toInt() ?: 0,
                    lapMessage = row["lap_message"],
                    npcText = row["npc_text"],
                )
            }
        }
    }
}
