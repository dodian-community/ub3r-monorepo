package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.systems.ui.dialogue.DialogueEmote

internal object Aubury {
    // Stats: 637: r=0 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> =
        spawnEntries(
            npcId = 637,
            point(2594, 3104),
            point(3253, 3402),
        )
    val npcIds: IntArray = npcIdsFromEntries(entries)
    val definition =
        npcPlugin("Aubury") {
            ids(*npcIds)
            spawns(entries)
            ownsSpawns(true)
            options {
                talkTo("talk-to") {
                    npc("Do you want to buy some runes?", DialogueEmote.EVIL1)
                    choice("Select an Option") {
                        option("Yes please!") {
                            finishThen {
                                openShop(9)
                            }
                        }
                        option("No thank you, then.") {
                            player("Oh it's a rune shop. No thank you, then.")
                            npc("Well, if you find someone who does want runes, send them my way.")
                        }
                    }
                }

                trade {
                    openShop(9)
                }

                teleportOption("teleport") {
                    whenCondition(
                        predicate = { balloonsEventActive() },
                        thenBlock = {
                            teleport(3045, 3372, 0, message = "Welcome to the party room!")
                        },
                    ) otherwise {
                        teleport(3086, 3488, 0, random = 2, message = "Welcome to Edgeville!")
                    }
                }
            }
        }
}
