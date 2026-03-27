package net.dodian.uber.game.content.entities.npcs.spawns

internal object Banker {
    val entries: List<NpcSpawnDef> = BankerGenerated.entries
    val npcIds: IntArray = npcIdsFromEntries(entries, 395, 7677)
    val definition =
        npcPlugin("Banker") {
            spawns(entries)
            ids(*npcIds)
            options {
                talkTo {
                    npc("Good day, how can I help you?")
                    choice("What would you like to say?") {
                        "I'd like to access my bank account, please." {
                            finishThen {
                                openBank()
                            }
                        }
                        "I'd like to check my PIN settings." {
                            npc("Pins have not been implemented yet.")
                        }
                    }
                }

                trade(label = "bank") {
                    openBank()
                }
            }
        }
}
