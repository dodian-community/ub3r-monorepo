package net.dodian.uber.game.npc

internal object FishingSpot4 : NpcFamily by npcFamily("Fishing spot", 1514, block = {
    server {
        deathAnimation = 2304
        hitpoints = 250
    }

    spawns {
        spawn(2601, 3422)
        spawn(2604, 3417)
    }
})
