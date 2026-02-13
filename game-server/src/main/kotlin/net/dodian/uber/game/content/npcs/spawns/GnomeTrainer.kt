package net.dodian.uber.game.content.npcs.spawns

import net.dodian.uber.game.Server
import net.dodian.uber.game.content.dialogue.DialogueEmote
import net.dodian.uber.game.content.dialogue.DialogueOption
import net.dodian.uber.game.content.dialogue.DialogueService
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueIds
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRegistry
import net.dodian.uber.game.content.npcs.dialogue.core.DialogueRenderModule
import net.dodian.uber.game.content.npcs.dialogue.core.dialogue
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

internal object GnomeTrainer {
    // Stats: 6080: r=60 a=0 d=0 s=0 hp=0 rg=0 mg=0

    val entries: List<NpcSpawnDef> = listOf(
        NpcSpawnDef(npcId = 6080, x = 2474, y = 3439, z = 0, face = 0),
        NpcSpawnDef(npcId = 6080, x = 3002, y = 3931, z = 0, face = 0),
        NpcSpawnDef(npcId = 6080, x = 2547, y = 3554, z = 0, face = 0),
    )

    val npcIds: IntArray = entries.map { it.npcId }.distinct().toIntArray()

    fun onFirstClick(client: Client, npc: Npc): Boolean {
        if (npc.slot >= Server.npcManager.gnomeSpawn) {
            return false
        }
        DialogueService.start(client) {
            npcChat(npc.id, DialogueEmote.DEFAULT, "Fancy meeting you here maggot.", "If you have any agility ticket,", "I would gladly take them from you.")
            options(
                title = "Trade in tickets or teleport to agility course?",
                DialogueOption("Trade in tickets.") {
                    action { c -> c.spendTickets() }
                    finish()
                },
                DialogueOption("Another course, please.") {
                    val atWilderness = client.skillX == 3002 && client.skillY == 3931
                    val atBarbarian = client.skillX == 2547 && client.skillY == 3554
                    val courseOptions = if (atWilderness) {
                        arrayOf("Gnome", "Barbarian", "Stay here")
                    } else if (atBarbarian) {
                        arrayOf("Gnome", "Wilderness", "Stay here")
                    } else {
                        arrayOf("Barbarian", "Wilderness", "Stay here")
                    }
                    options(
                        title = "Which course do you wish to be taken to?",
                        DialogueOption(courseOptions[0]) {
                            action { c ->
                                if (atWilderness) c.teleportTo(2474, 3438, 0)
                                else if (atBarbarian) c.teleportTo(2474, 3438, 0)
                                else c.teleportTo(2547, 3553, 0)
                            }
                            finish()
                        },
                        DialogueOption(courseOptions[1]) {
                            action { c ->
                                if (atWilderness) c.teleportTo(2547, 3553, 0)
                                else if (atBarbarian) c.teleportTo(3002, 3932, 0)
                                else c.teleportTo(3002, 3932, 0)
                            }
                            finish()
                        },
                        DialogueOption(courseOptions[2]) { finish() },
                    )
                },
            )
        }
        return true
    }
}

object AgilityDialogueModule : DialogueRenderModule {

    override fun register(builder: DialogueRegistry.Builder) {
        builder.handle(DialogueIds.Agility.TICKET_GREETING) { c ->
            dialogue(c) {
                npcChat(c.NpcTalkTo, 591, "Fancy meeting you here maggot.", "If you have any agility ticket,", "I would gladly take them from you.")
                setNextDiag(DialogueIds.Agility.TICKET_OPTIONS)
            }
        }

        builder.handle(DialogueIds.Agility.TICKET_OPTIONS) { c ->
            c.send(net.dodian.uber.game.netty.listener.out.Frame171(1, 2465))
            c.send(net.dodian.uber.game.netty.listener.out.Frame171(0, 2468))
            c.send(net.dodian.uber.game.netty.listener.out.SendString("Trade in tickets or teleport to agility course?", 2460))
            c.send(net.dodian.uber.game.netty.listener.out.SendString("Trade in tickets.", 2461))
            c.send(net.dodian.uber.game.netty.listener.out.SendString("Another course, please.", 2462))
            c.sendFrame164(2459)
            c.NpcDialogueSend = true
            true
        }

        builder.handle(DialogueIds.Agility.COURSE_TRAVEL) { c ->
            val type = if (c.skillX == 3002 && c.skillY == 3931) 3 else if (c.skillX == 2547 && c.skillY == 3554) 2 else 1
            val typeGnome = arrayOf("Which course do you wish to be taken to?", "Barbarian", "Wilderness", "Stay here")
            val typeBarbarian = arrayOf("Which course do you wish to be taken to?", "Gnome", "Wilderness", "Stay here")
            val typeWilderness = arrayOf("Which course do you wish to be taken to?", "Gnome", "Barbarian", "Stay here")
            dialogue(c) {
                options(*(if (type == 3) typeWilderness else if (type == 2) typeBarbarian else typeGnome))
            }
        }
    }
}
