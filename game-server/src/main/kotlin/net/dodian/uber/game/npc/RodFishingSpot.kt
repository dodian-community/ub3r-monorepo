package net.dodian.uber.game.npc

internal object RodFishingSpot : NpcFamily by npcFamily("Rod Fishing spot", 1506, block = {
    server {
        deathAnimation = 2304
        hitpoints = 250
    }

    spawns {
        spawn(2605, 3421)
        spawn(2844, 3429)
    }
})
