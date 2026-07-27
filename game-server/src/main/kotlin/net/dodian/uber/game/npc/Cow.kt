package net.dodian.uber.game.npc

import net.dodian.uber.game.api.content.dialogue.DialogueEmote
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.systems.event.GameEventService
import net.dodian.uber.game.engine.systems.event.ServerEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object Cow : NpcFamily by npcFamily("Cow", 5842, block = {

    definition {
        name = "Cow"
        examine = "Meow meow I am a cow!"
    }

    server {
        attackAnimation = 5849
        defenceAnimation = 5850
        deathAnimation = 5851
        respawnTicks = 30
        attack = 1
        strength = 1
        defence = 1
        hitpoints = 8
        magic = 1
        ranged = 1
        fightsBack = false
    }

    options {
        first("Talk-to", handler = ::handleCowEventReward)
        attack(handler = { _, _ -> true })
    }

    spawns {
        spawn(2601, 3113)
        spawn(2602, 3116)
        spawn(2604, 3114)
        spawn(2605, 3116)
        spawn(2608, 3112)
        spawn(2609, 3115)
    }
})

@Suppress("UNUSED_PARAMETER")
private fun handleCowEventReward(client: Client, npc: Npc): Boolean {
    val alreadyClaimed = client.checkItem(EASTER_RING)
    val eventActive = GameEventService.isActive(ServerEvent.EASTER)
    DialogueService.start(client) {
        when {
            eventActive && !alreadyClaimed -> {
                npcChat(npc.id, DialogueEmote.ANNOYED, "Here take a easter ring for all your troubles.", "Enjoy your stay at Dodian.")
                finish {
                    it.addItem(EASTER_RING, 1)
                    it.checkItemUpdate()
                }
            }
            alreadyClaimed -> {
                npcChat(npc.id, DialogueEmote.ANNOYED, "You already got the ring.")
                finish()
            }
            else -> {
                npcChat(npc.id, DialogueEmote.ANNOYED, "The Easter event is not active right now.")
                finish()
            }
        }
    }
    return true
}

private const val EASTER_RING = 7927
