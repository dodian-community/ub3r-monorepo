package net.dodian.uber.game.npc

internal object PiratePete : NpcFamily by npcFamily("Pirate Pete", 3389, block = {
    definition {
        examine = "Converts grass to beef.  Where beef comes from.  Beefy!"
        name = "Pirate Pete"
    }

    spawns {
        spawn(2605, 3399)
    }
})
