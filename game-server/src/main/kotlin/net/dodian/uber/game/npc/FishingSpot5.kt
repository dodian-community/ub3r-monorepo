package net.dodian.uber.game.npc

internal object FishingSpot5 : NpcFamily by npcFamily("Fishing spot", 1520, block = {
    server {
        deathAnimation = 2304
    }

    spawns {
        spawn(2598, 3425)
    }
})
