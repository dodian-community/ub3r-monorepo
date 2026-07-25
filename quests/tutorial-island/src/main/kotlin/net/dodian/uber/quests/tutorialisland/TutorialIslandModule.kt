package net.dodian.uber.quests.tutorialisland

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.quests.Quest
import net.dodian.uber.game.api.plugin.quests.QuestPlayer
import net.dodian.uber.game.api.plugin.quests.QuestPlugin
import net.dodian.uber.game.api.plugin.quests.QuestPluginDefinition
import net.dodian.uber.game.api.plugin.quests.QuestState
import net.dodian.uber.game.api.plugin.quests.manifest
import net.dodian.uber.game.api.plugin.quests.questPlugin
import net.dodian.uber.game.api.plugin.quests.rewards
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.quests.runtime.QuestStageHandler
import net.dodian.uber.quests.runtime.QuestStageMachine

/**
 * Worked example proving the ported quest-engine mechanics end to end, using a
 * few of the real early OSRS Tutorial Island beats (talk to the Gielinor
 * Guide, leave his house, talk to the Survival Expert) as a demonstration for
 * how to build further content on this API - not a full recreation.
 *
 * [GIELINOR_GUIDE_NPC_ID]/[SURVIVAL_EXPERT_NPC_ID] and the item ids are
 * cache-verified (see `TutorialIslandNpcs.kt` in game-server). [TUTORIAL_ENTRY]
 * and [GUIDE_HOUSE_DOOR_OBJECT_ID] are now live-verified: confirmed in-game
 * via `::tele 3094 3107 0`, and the door id came straight off a real click
 * (`ConsoleAudit ... objectId=9398 | pos=3098,3107,0 | route=REACHED |
 * source=cache-action-noop`) - the island's physical layout evidently matches
 * 2009scape's coordinates closely, only the NPCs/interior were reworked.
 * The Survival Expert's spawn (`TutorialIslandNpcs.kt`) is still an estimate
 * (2009scape's relative offset from the entry, not independently
 * re-verified) - confirm by walking to her once spawned; `::tutorial next`
 * remains available if anything is still off.
 */
object TutorialIslandModule : QuestPlugin {
    private const val GIELINOR_GUIDE_NPC_ID = 9476
    private const val SURVIVAL_EXPERT_NPC_ID = 9477
    private const val BRONZE_AXE_ITEM_ID = 1351
    private const val TINDERBOX_ITEM_ID = 590

    // Verified live: game-server/src/main/kotlin/net/dodian/uber/game/npc/TutorialIslandNpcs.kt
    private const val GUIDE_HOUSE_DOOR_OBJECT_ID = 9398

    private val TUTORIAL_ENTRY = SkillPosition(3094, 3107, 0) // verified live
    private val OUTSIDE_GUIDE_HOUSE = SkillPosition(3098, 3107, 0) // verified live (the door tile itself)
    private val GRADUATION_SPOT = SkillPosition(3222, 3218, 0) // Lumbridge courtyard hand-off point

    const val QUEST_KEY = "tutorial_island"

    val quest: Quest = Quest.register(
        id = 500,
        key = QUEST_KEY,
        displayName = "Tutorial Island",
        maxSteps = 3,
        rewards = rewards { xp(Skill.HITPOINTS, 0) }, // placeholder: attach real starter rewards here
    )

    private val stages = QuestStageMachine(
        mapOf(
            1 to QuestStageHandler { player, _, resuming ->
                player.world.teleport(TUTORIAL_ENTRY)
                player.ui.message(
                    if (!resuming) "The Gielinor Guide welcomes you. Leave through the door to continue."
                    else "Leave through the door to continue.",
                )
            },
            2 to QuestStageHandler { player, _, resuming ->
                player.world.teleport(OUTSIDE_GUIDE_HOUSE)
                player.ui.message(
                    if (!resuming) "You step outside. Find the Survival Expert nearby and talk to her."
                    else "Find the Survival Expert nearby and talk to her.",
                )
            },
            3 to QuestStageHandler { player, _, _ ->
                player.world.teleport(GRADUATION_SPOT)
                player.ui.message("Tutorial complete! Welcome to the world.")
            },
        ),
    )

    /** Re-establishes whatever the player's current stage needs - call this on login. */
    fun resume(player: QuestPlayer) {
        val stage = quest.getQuestStage(player)
        if (stage in 1..quest.maxSteps) stages.dispatch(player, stage, resuming = true)
    }

    /** Starts the quest if not yet started, otherwise resumes at the current stage. Shared by the quest-tab link and `::tutorial`. */
    fun startOrResume(player: QuestPlayer) {
        if (quest.questState(player) == QuestState.NOT_STARTED) {
            val stage = quest.advanceQuestStage(player)
            stages.dispatch(player, stage)
        } else {
            resume(player)
        }
    }

    /** Dev-only escape hatch (`::tutorial next`): force-advances one stage without requiring the real interaction. */
    fun forceAdvance(player: QuestPlayer): Int {
        val stage = quest.advanceQuestStage(player)
        stages.dispatch(player, stage)
        return stage
    }

    override val definition: QuestPluginDefinition = questPlugin("Tutorial Island") {
        npcClick(option = 1, GIELINOR_GUIDE_NPC_ID) { interaction ->
            val player = interaction.player
            if (quest.questState(player) == QuestState.NOT_STARTED) {
                val stage = quest.advanceQuestStage(player)
                stages.dispatch(player, stage)
            } else {
                player.ui.message("Leave through the door to continue.")
            }
            true
        }

        // Returns false either way so the door's normal open/close behavior
        // still runs - this only piggybacks on the click to advance the quest.
        objectClick(option = 1, GUIDE_HOUSE_DOOR_OBJECT_ID) { interaction ->
            val player = interaction.player
            if (quest.questState(player) == QuestState.IN_PROGRESS && quest.getQuestStage(player) == 1) {
                val stage = quest.advanceQuestStage(player)
                stages.dispatch(player, stage)
            }
            false
        }

        npcClick(option = 1, SURVIVAL_EXPERT_NPC_ID) { interaction ->
            val player = interaction.player
            when {
                quest.questState(player) == QuestState.IN_PROGRESS && quest.getQuestStage(player) == 2 -> {
                    player.inventory.add(BRONZE_AXE_ITEM_ID)
                    player.inventory.add(TINDERBOX_ITEM_ID)
                    player.ui.message("The Survival Expert hands you a bronze axe and a tinderbox.")
                    val stage = quest.advanceQuestStage(player)
                    stages.dispatch(player, stage)
                }
                quest.isQuestCompleted(player) -> player.ui.message("You've already finished Tutorial Island.")
                else -> player.ui.message("Talk to the Gielinor Guide first.")
            }
            true
        }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = "quest.tutorial_island",
        owner = "gameplay",
        version = "1.0.0",
        maturity = ContentMaturity.ALPHA,
    )
}
