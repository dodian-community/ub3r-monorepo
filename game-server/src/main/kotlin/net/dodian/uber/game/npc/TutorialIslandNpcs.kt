package net.dodian.uber.game.npc

/**
 * Spawns for the two Tutorial Island NPCs the `quests/tutorial-island` module
 * talks to (see [net.dodian.uber.quests.tutorialisland.TutorialIslandModule]).
 * Both NPC ids are cache-verified (`game-server/data/mappings/npc.rscm`:
 * `gielinor_guide=9476`, `survival_expert=9477`, both "1 actions: Talk-to").
 *
 * Neither NPC previously spawned anywhere in ub3r. Click handling (option 1,
 * "Talk-to") is owned entirely by the quest engine's `npcClick` binding in
 * `TutorialIslandModule` - `QuestInteractionDispatcher` runs before this
 * family's own (unused) options would, so no `options { talkTo(...) }` is
 * registered here; this file only makes the NPCs exist in the world.
 *
 * The Gielinor Guide's spawn is live-verified: `(3094, 3107, 0)` is the
 * confirmed entry tile (`::tele 3094 3107 0`), and the island's layout turned
 * out to match 2009scape's coordinates closely (only the NPCs/interior were
 * reworked in OSRS's 2017 update) - the guide is placed right at the entry,
 * mirroring 2009scape's `RUNESCAPE_GUIDE` spawn one tile over.
 *
 * The Survival Expert's spawn is still an estimate - 2009scape's relative
 * offset from the entry, not independently re-verified against this cache.
 * Walk there once she's spawned and adjust if she's not visible/reachable.
 */
internal object GielinorGuide : NpcFamily by npcFamily("Gielinor Guide", 9476, block = {
    definition {
        examine = "He can help you learn the basics."
    }

    spawns {
        spawn(3093, 3107, walkRadius = 0)
    }
})

internal object TutorialSurvivalExpert : NpcFamily by npcFamily("Survival Expert", 9477, block = {
    definition {
        examine = "She can teach you survival skills."
    }

    spawns {
        spawn(3103, 3096, walkRadius = 0)
    }
})
