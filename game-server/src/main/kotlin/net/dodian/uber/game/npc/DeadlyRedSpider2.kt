package net.dodian.uber.game.npc

internal object DeadlyRedSpider2 : NpcFamily by npcFamily("Deadly red spider", 3021, block = {
    definition {
        examine = "I think this spider has been genetically modified."
    }

    server {
        defenceAnimation = 5328
        attackAnimation = 5327
        deathAnimation = 5329
        attack = 50
        strength = 68
        defence = 38
        hitpoints = 75
        magic = 1
        ranged = 1
    }

    spawns {
        // This cave uses the same revision-218 NPC definition (3021) but retains the
        // historical weaker combat profile through spawn overrides. Keeping it in the same
        // family prevents two global NpcContentDefinitions from claiming one cache ID.
        spawn(
            3210,
            9897,
            hitpoints = 60,
            attack = 19,
            strength = 21,
            defence = 16,
            magic = 1,
            ranged = 1,
            defenceAnimation = 5573,
            deathAnimation = 2304,
        )
        spawn(
            3213,
            9890,
            hitpoints = 60,
            attack = 19,
            strength = 21,
            defence = 16,
            magic = 1,
            ranged = 1,
            defenceAnimation = 5573,
            deathAnimation = 2304,
        )
        spawn(3591, 3479)
        spawn(3593, 3476)
        spawn(3593, 3483)
        spawn(3595, 3473)
        spawn(3596, 3477)
        spawn(3596, 3481)
        spawn(3598, 3474)
        spawn(3600, 3481)
        spawn(3601, 3472)
        spawn(3601, 3478)
        spawn(3603, 3475)
        spawn(3603, 3484)
        spawn(3604, 3481)
    }
})
