package net.dodian.uber.game.npc

internal object CaveBugLarva : NpcFamily by npcFamily("Cave bug larva", 482, block = {
    definition {
        examine = "I guess he sells what he steals...?"
    }

    server {
        deathAnimation = 2304
    }

    spawns {
        spawn(2463, 4365)
    }
})
