package net.dodian.uber.game.npc

import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.skills.slayer.SlayerModule

internal object Duradel : NpcFamily by npcFamily("Duradel", 405, block = {
    definition {
        examine = "He looks dangerous!"
    }

    server {
        deathAnimation = 2304
    }

    options {
        talkTo(handler = ::handleSlayerMasterTalkTo)
        third("assignment", ::handleSlayerMasterAssignment)
    }

    spawns {
        spawn(2606, 3398)
    }
})

// Shadowed in practice by SlayerModule's own npcClick(option=1/3, 405) bindings
// (SkillInteractionDispatcher is tried before NpcContentRegistry) - kept functionally correct
// rather than stubbed, since these registrations still drive the client-visible "talk-to"/
// "assignment" right-click menu labels (see the Tanner.kt/Zahur.kt precedent).
private fun handleSlayerMasterTalkTo(client: Client, npc: Npc): Boolean {
    SlayerModule.startIntro(client.asSkillPlayer(), npc.id)
    return true
}

private fun handleSlayerMasterAssignment(client: Client, npc: Npc): Boolean {
    SlayerModule.assignTask(client.asSkillPlayer(), npc.id)
    return true
}
