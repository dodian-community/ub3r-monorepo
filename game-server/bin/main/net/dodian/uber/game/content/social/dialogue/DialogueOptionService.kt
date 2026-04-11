package net.dodian.uber.game.content.social.dialogue

import net.dodian.uber.game.Server
import net.dodian.uber.game.systems.world.item.Ground
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.content.skills.agility.AgilityData
import net.dodian.uber.game.content.skills.agility.Agility
import net.dodian.uber.game.content.skills.agility.AgilityTravel
import net.dodian.uber.game.content.skills.herblore.HerbloreData
import net.dodian.uber.game.content.skills.thieving.PyramidPlunder
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendFrame27
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.utilities.Utils
import kotlin.math.ceil
import kotlin.math.min

object DialogueOptionService {

    @JvmStatic
    fun triggerChat(c: net.dodian.uber.game.model.entity.player.Client, button: Int) {
        fun setNextDialogue(id: Int) = DialogueService.setNextDialogueId(c, id)
        fun setDialogue(id: Int) = DialogueService.setDialogueId(c, id)
        fun setDialogueSent(sent: Boolean) = DialogueService.setDialogueSent(c, sent)
        fun resetDialogue() = DialogueService.resetDialogueState(c)

        if (c.playerPotato.isNotEmpty()) {
            if (c.playerPotato[0] == 2 && c.playerPotato[3] == 1) {
                c.send(RemoveInterfaces())
                val tempNpc: Npc = Server.npcManager.getNpc(c.playerPotato[1])
                val npcId = c.playerPotato[2]
                resetDialogue()

                if (button == 1) {
                    c.sendMessage("NPC spawn DB deletion is disabled after hard cutover.")
                    c.sendMessage("Remove/update spawns in Kotlin NPC content files instead.")
                    // TODO(npc-hard-cutover): legacy SQL delete flow kept for rollback.
//                    try {
//                        val connection = dbConnection
//                        val statement = connection.createStatement()
//                        try {
//                            val sql = "delete from " + DbTables.GAME_NPC_SPAWNS + " where id='" + npcId + "' && x='" + tempNpc.position.x + "' && y='" + tempNpc.position.y + "' && height='" + tempNpc.position.z + "'"
//                            if (statement.executeUpdate(sql) < 1) {
//                                c.sendMessage("This npc has already been removed!")
//                            } else {
//                                tempNpc.die()
//                                Replaced with GameEventScheduler after legacy event scheduler removal.
//                                    override fun execute() {
//                                        Server.npcManager.npcs.remove(tempNpc)
//                                        stop()
//                                    }
//                                })
//                                c.sendMessage("You removed this npc spawn!")
//                            }
//                        } finally {
//                            statement.close()
//                            connection.close()
//                        }
//                    } catch (e: Exception) {
//                        c.sendMessage("Something went wrong in removing this npc!")
//                    }
                } else if (button == 2) {
                    if (tempNpc.data.drops.isNotEmpty()) {
                        var line = 8147
                        var totalChance = 0.0
                        c.clearQuestInterface()
                        c.sendString("@dre@Drops for @blu@" + tempNpc.npcName() + "@bla@(@gre@" + npcId + "@bla@)", 8144)
                        val text = ArrayList<String>()

                        for (drop in tempNpc.data.drops) {
                            if (drop.chance >= 100.0) {
                                text.add((if (drop.minAmount == drop.maxAmount) drop.minAmount.toString() else drop.minAmount.toString() + " - " + drop.maxAmount) + " " + c.getItemName(drop.id) + "(" + drop.id + ")")
                            }
                        }
                        for (drop in tempNpc.data.drops) {
                            if (drop.chance < 100.0) {
                                val minAmount = drop.minAmount
                                val maxAmount = drop.maxAmount
                                val itemId = drop.id
                                val chance = drop.chance
                                text.add((if (minAmount == maxAmount) minAmount.toString() else "$minAmount - $maxAmount") + " " + c.getItemName(itemId) + "(" + itemId + ") " + chance + "% (1:" + Math.round(100.0 / chance) + ")" + if (drop.rareShout()) ", YELL" else "")
                                totalChance += chance
                            }
                        }
                        for (txt in text) {
                            c.sendString(txt, line)
                            line++
                            if (line == 8196) {
                                line = 12174
                            }
                        }
                        c.sendString(if (totalChance > 100.0) "You are currently " + (totalChance - 100.0) + " % over!" else "Total drops %: $totalChance%", 8145)
                        c.sendString(if (totalChance < 0.0 || totalChance >= 100.0) "" else "Nothing " + (100.0 - totalChance) + "%", line)
                        c.sendQuestSomething(8143)
                        c.openInterface(8134)
                    } else {
                        c.sendMessage("Npc " + tempNpc.npcName() + " (" + npcId + ") has no assigned drops!")
                    }
                } else if (button == 3) {
                    Server.npcManager.reloadDrops(c, npcId)
                } else if (button == 4) {
                    tempNpc.showConfig(c)
                } else if (button == 5) {
                    Server.npcManager.reloadAllData(c, npcId)
                }
                c.playerPotato.clear()
                return
            }
        }

        val dialogueId = DialogueService.currentDialogueId(c)
        val npcTalkTo = DialogueService.activeNpcId(c)

        if (c.convoId == 0) {
            if (button == 1) {
                c.openUpBank()
            } else {
                setNextDialogue(8)
            }
        }

        if (dialogueId == 12) {
            setNextDialogue(if (button == 1) 13 else if (button == 2) 31 else if (button == 4) 14 else 34)
        }

        if (dialogueId == 16) {
            if (button == 1) {
                val checkTask = net.dodian.uber.game.content.skills.slayer.SlayerTaskDefinition.forOrdinal(c.slayerData[1])
                if (checkTask != null) {
                    for (i in checkTask.npcId.indices) {
                        for (slot in 0 until c.monsterName.size) {
                            if (c.monsterName[slot].equals(Server.npcManager.getName(checkTask.npcId[i]), ignoreCase = true)) {
                                c.monsterCount[slot] = 0
                            }
                        }
                    }
                    c.sendMessage(checkTask.textRepresentation + " have now been reseted!")
                }
            }
            c.send(RemoveInterfaces())
        }

        if (dialogueId == 20) {
            c.faceNpc(-1)
            var missing = 5000
            val amount = c.getInvAmt(995).toLong() + c.getBankAmt(995)
            if (amount >= 5000L) {
                if (c.getInvAmt(995) >= missing) {
                    c.deleteItem(995, missing)
                } else {
                    missing -= c.getInvAmt(995)
                    c.deleteItem(995, c.getInvAmt(995))
                }
                if (missing > 0) {
                    c.deleteItemBank(995, missing)
                }
                c.checkItemUpdate()
            }

            val carpet = AgilityTravel(c)
            if (button >= 4) {
                c.showPlayerChat(arrayOf("No, thank you."), 614)
            } else if (amount < 5000) {
                DialogueService.showNpcChat(c, npcTalkTo, 594, arrayOf("You do not have enough coins to do this!", "You are currently missing " + (missing - amount) + " coins."))
            } else if (npcTalkTo == 17) {
                carpet.sophanem(button - 1)
            } else if (npcTalkTo == 19) {
                carpet.bedabinCamp(button - 1)
            } else if (npcTalkTo == 20) {
                carpet.pollnivneach(button - 1)
            } else if (npcTalkTo == 22) {
                carpet.nardah(button - 1)
            }
        }

        if (dialogueId == 32) {
            if (button == 1) {
                setNextDialogue(33)
            } else {
                c.send(RemoveInterfaces())
            }
        }

        if (dialogueId == 35) {
            if (button == 1) {
                setNextDialogue(36)
            } else {
                c.showPlayerChat(arrayOf("No, thank you."), 614)
            }
        }

        if (c.convoId == 3) {
            if (button == 1) {
                c.openUpShopRouted(9)
            } else {
                c.send(RemoveInterfaces())
            }
        }

        if (c.convoId == 4) {
            if (button == 1) {
                c.openUpShopRouted(22)
            } else {
                c.send(RemoveInterfaces())
            }
        }

        if (dialogueId == 163) {
            if (button == 1) {
                c.spendTickets()
            } else {
                setNextDialogue(164)
            }
        } else if (dialogueId == 164) {
            val type =
                if (c.interactionAnchorX == 3002 && c.interactionAnchorY == 3931) 3
                else if (c.interactionAnchorX == 2547 && c.interactionAnchorY == 3554) 2
                else 1
            if (button == 1) {
                c.teleportTo(if (type == 1) 2547 else 2474, if (type == 1) 3553 else 3438, 0)
            } else if (button == 2) {
                c.teleportTo(if (type == 3) 2547 else 3002, if (type == 3) 3553 else 3932, 0)
            }
            c.send(RemoveInterfaces())
        } else if (dialogueId == 3649) {
            if (button == 1) {
                c.setTravelMenu()
            } else if (button == 2) {
                c.showPlayerChat(arrayOf("No thank you."), 614)
            }
            resetDialogue()
        } else if (dialogueId == 2346) {
            if (button == 1) {
                setNextDialogue(2347)
            } else if (button == 2) {
                if (!c.checkUnlock(0)) {
                    val maximumTickets = 10
                    val minimumTicket = 1
                    val ticketValue = 300_000
                    var missing = (maximumTickets - minimumTicket) * ticketValue
                    if (!c.playerHasItem(621, minimumTicket)) {
                        DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You need a minimum of $minimumTicket ship ticket", "to unlock permanent!"))
                        return
                    }
                    missing -= (c.getInvAmt(621) - minimumTicket) * ticketValue
                    if (missing > 0) {
                        if (c.getInvAmt(995) >= missing) {
                            c.deleteItem(621, min(c.getInvAmt(621), maximumTickets))
                            c.deleteItem(995, missing)
                            c.addUnlocks(0, c.checkUnlockPaid(0).toString(), "1")
                            DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("Thank you for the payment.", "You may enter freely into my dungeon."))
                        } else {
                            DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You do not have enough coins to do this!", "You are currently missing " + (missing - c.getInvAmt(995)) + " coins or", ceil((missing - c.getInvAmt(995)) / 300_000.0).toInt().toString() + " ship tickets."))
                        }
                    } else {
                        c.deleteItem(621, maximumTickets)
                        c.addUnlocks(0, c.checkUnlockPaid(0).toString(), "1")
                        DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("Thank you for the ship tickets.", "You may enter freely into my dungeon."))
                    }
                    c.checkItemUpdate()
                }
            } else if (button == 3) {
                c.showPlayerChat(arrayOf("I do not want anything."), 614)
            } else {
                c.send(RemoveInterfaces())
            }
        } else if (dialogueId == 2347) {
            if (c.checkUnlockPaid(0) > 0 || c.checkUnlock(0)) {
                DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You have already paid me.", "Please step into my dungeon."))
            } else if (button == 1) {
                if (c.getInvAmt(621) > 0 || c.getBankAmt(621) > 0) {
                    c.addUnlocks(0, "1", if (c.checkUnlock(0)) "1" else "0")
                    if (c.getInvAmt(621) > 0) {
                        c.deleteItem(621, 1)
                    } else {
                        c.deleteItemBank(621, 1)
                    }
                    c.checkItemUpdate()
                    DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You can now step into the dungeon."))
                } else {
                    DialogueService.showNpcChat(c, npcTalkTo, 596, arrayOf("You need a ship ticket to enter my dungeon!"))
                }
            } else if (button == 2) {
                val amount = c.getInvAmt(995).toLong() + c.getBankAmt(995)
                val total = 300_000
                if (amount >= total) {
                    c.addUnlocks(0, "1", if (c.checkUnlock(0)) "1" else "0")
                    val remain = total - c.getInvAmt(995)
                    c.deleteItem(995, total)
                    if (remain > 0) {
                        c.deleteItemBank(995, remain)
                    }
                    DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You can now step into the dungeon."))
                } else {
                    DialogueService.showNpcChat(c, npcTalkTo, 596, arrayOf("You need atleast " + (300_000 - amount) + " more coins to enter my dungeon!"))
                }
            }
        } else if (dialogueId == 2181) {
            if (button == 1) {
                setNextDialogue(2182)
            } else if (button == 2) {
                if (!c.checkUnlock(1)) {
                    val maximumTickets = 20
                    val minimumTicket = 3
                    val ticketValue = 300_000
                    var missing = (maximumTickets - minimumTicket) * ticketValue
                    if (!c.playerHasItem(621, minimumTicket)) {
                        DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You need a minimum of $minimumTicket ship ticket", "to unlock permanent!"))
                        return
                    }
                    missing -= (c.getInvAmt(621) - minimumTicket) * ticketValue
                    if (missing > 0) {
                        if (c.getInvAmt(995) >= missing) {
                            c.deleteItem(621, min(c.getInvAmt(621), maximumTickets))
                            c.deleteItem(995, missing)
                            c.checkItemUpdate()
                            c.addUnlocks(1, c.checkUnlockPaid(1).toString(), "1")
                            DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("Thank you for the payment.", "You may enter freely into my cave."))
                        } else {
                            DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You do not have enough coins to do this!", "You are currently missing " + (missing - c.getInvAmt(995)) + " coins or", ceil((missing - c.getInvAmt(995)) / 300_000.0).toInt().toString() + " ship tickets."))
                        }
                    } else {
                        c.deleteItem(621, maximumTickets)
                        c.checkItemUpdate()
                        c.addUnlocks(1, c.checkUnlockPaid(1).toString(), "1")
                        DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("Thank you for the ship tickets.", "You may enter freely into my cave."))
                    }
                }
            } else if (button == 3) {
                c.showPlayerChat(arrayOf("I do not want anything."), 614)
            } else {
                c.send(RemoveInterfaces())
            }
        } else if (dialogueId == 2182) {
            if (c.checkUnlockPaid(1) > 0 || c.checkUnlock(1)) {
                DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You have already paid me.", "Please step into my cave."))
            } else if (button == 1) {
                if (c.getInvAmt(621) > 0 || c.getBankAmt(621) > 0) {
                    c.addUnlocks(1, "1", if (c.checkUnlock(1)) "1" else "0")
                    if (c.getInvAmt(621) > 0) {
                        c.deleteItem(621, 1)
                    } else {
                        c.deleteItemBank(621, 1)
                    }
                    c.checkItemUpdate()
                    DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You can now step into the cave."))
                } else {
                    DialogueService.showNpcChat(c, npcTalkTo, 596, arrayOf("You need a ship ticket to enter my cave!"))
                }
            } else if (button == 2) {
                val amount = c.getInvAmt(995).toLong() + c.getBankAmt(995)
                val total = 300_000
                if (amount >= total) {
                    c.addUnlocks(1, "1", if (c.checkUnlock(1)) "1" else "0")
                    val remain = total - c.getInvAmt(995)
                    c.deleteItem(995, total)
                    if (remain > 0) {
                        c.deleteItemBank(995, remain)
                    }
                    c.checkItemUpdate()
                    DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("You can now step into the cave."))
                } else {
                    DialogueService.showNpcChat(c, npcTalkTo, 596, arrayOf("You need atleast " + (300_000 - amount) + " more coins to enter my cave!"))
                }
            }
        } else if (dialogueId == 4759) {
            if (button == 1) {
                c.send(RemoveInterfaces())
                c.openUpShop(39)
            } else if (button == 2) {
                setNextDialogue(4756)
            } else if (button == 3) {
                setNextDialogue(4757)
            } else {
                c.send(RemoveInterfaces())
            }
        } else if (dialogueId == 6483) {
            if (button == 1) {
                setNextDialogue(dialogueId + 1)
            } else if (button == 2) {
                c.showPlayerChat(arrayOf("No thank you."), 614)
            }
        } else if (dialogueId == 8053) {
            if (button == 1) {
                c.send(RemoveInterfaces())
                c.openUpShop(55)
            } else {
                c.send(RemoveInterfaces())
            }
        } else if (dialogueId == 10000) {
            if (button == 1) {
                if (c.playerHasItem(6161) && c.playerHasItem(6159)) {
                    c.deleteItem(6159, 1)
                    c.deleteItem(6161, 1)
                    c.addItem(6128, 1)
                    c.checkItemUpdate()
                    c.showPlayerChat(arrayOf("I just made Rock-shell head."), 614)
                } else {
                    c.showPlayerChat(arrayOf("I need the following items:", c.getItemName(6161) + " and " + c.getItemName(6159)), 614)
                }
            } else if (button == 2) {
                if (c.playerHasItem(6157) && c.playerHasItem(6159) && c.playerHasItem(6161)) {
                    c.deleteItem(6157, 1)
                    c.deleteItem(6159, 1)
                    c.deleteItem(6161, 1)
                    c.addItem(6129, 1)
                    c.checkItemUpdate()
                    c.showPlayerChat(arrayOf("I just made Rock-shell body."), 614)
                } else {
                    c.showPlayerChat(arrayOf("I need the following items:", c.getItemName(6161) + ", " + c.getItemName(6159) + " and " + c.getItemName(6157)), 614)
                }
            } else if (button == 3) {
                if (c.playerHasItem(6159) && c.playerHasItem(6157)) {
                    c.deleteItem(6157, 1)
                    c.deleteItem(6159, 1)
                    c.addItem(6130, 1)
                    c.checkItemUpdate()
                    c.showPlayerChat(arrayOf("I just made Rock-shell legs."), 614)
                } else {
                    c.showPlayerChat(arrayOf("I need the following items:", c.getItemName(6159) + " and " + c.getItemName(6157)), 614)
                }
            } else if (button == 4) {
                if (c.playerHasItem(6161) && c.playerHasItem(6159)) {
                    c.deleteItem(6159, 1)
                    c.deleteItem(6161, 1)
                    c.addItem(6145, 1)
                    c.checkItemUpdate()
                    c.showPlayerChat(arrayOf("I just made Rock-shell boots."), 614)
                } else {
                    c.showPlayerChat(arrayOf("I need the following items:", c.getItemName(6161) + " and " + c.getItemName(6159)), 614)
                }
            } else if (button == 5) {
                if (c.playerHasItem(6161, 2)) {
                    c.deleteItem(6161, 1)
                    c.deleteItem(6161, 1)
                    c.addItem(6151, 1)
                    c.checkItemUpdate()
                    c.showPlayerChat(arrayOf("I just made Rock-shell gloves."), 614)
                } else {
                    c.showPlayerChat(arrayOf("I need two of " + c.getItemName(6161)), 614)
                }
            }
            setDialogueSent(true)
        } else if (dialogueId == 536) {
            if (button == 1) {
                val amount = c.getInvAmt(536).toLong() + c.getInvAmt(537) + c.getBankAmt(536)
                    var amt = AgilityData.KBD_ENTRANCE_BONE_AMOUNT
                if (amount >= 5) {
                    while (amt > 0) {
                        for (slot in 0 until 28) {
                            if (amt <= 0) break
                            if (c.playerItems[slot] - 1 == AgilityData.KBD_ENTRANCE_BONE_ID) {
                                c.deleteItem(AgilityData.KBD_ENTRANCE_BONE_ID, slot, 1)
                                amt--
                            }
                        }
                        for (slot in 0 until 28) {
                            if (c.playerItems[slot] - 1 == AgilityData.KBD_ENTRANCE_NOTED_BONE_ID) {
                                val toDelete = min(c.playerItemsN[slot], amt)
                                c.deleteItem(AgilityData.KBD_ENTRANCE_NOTED_BONE_ID, slot, toDelete)
                                amt -= toDelete
                                break
                            }
                        }
                        for (slot in c.bankItems.indices) {
                            if (c.bankItems[slot] - 1 == AgilityData.KBD_ENTRANCE_BONE_ID) {
                                c.bankItemsN[slot] -= amt
                                break
                            }
                        }
                        amt = 0
                    }
                    val agi = Agility(c)
                    agi.kbdEntrance()
                    c.checkItemUpdate()
                    c.sendMessage("You sacrifice 5 dragon bones!")
                } else {
                    c.sendMessage("You need to have 5 dragon bones to sacrifice!")
                }
                c.send(RemoveInterfaces())
            } else {
                c.send(RemoveInterfaces())
            }
        } else if (dialogueId == 3838) {
            if (button == 1) {
                c.send(RemoveInterfaces())
                c.XinterfaceID = 3838
                c.send(SendFrame27())
            } else {
                c.send(RemoveInterfaces())
            }
        } else if (dialogueId == 1177) {
            if (button == 1) {
                c.send(RemoveInterfaces())
                c.openUpShopRouted(19)
            } else if (button == 2) {
                setNextDialogue(1178)
            } else {
                c.showPlayerChat(arrayOf("Nevermind, I do not need anything."), 614)
            }
        } else if (dialogueId == 1178) {
            if (button >= 5) {
                c.showPlayerChat(arrayOf("Nevermind, I do not need anything."), 614)
            } else {
                c.send(RemoveInterfaces())
                resetDialogue()
                val doseDefinitions = HerbloreData.potionDoseDefinitions
                val amount = LongArray(doseDefinitions.size)
                val vials = LongArray(doseDefinitions.size)
                for (i in doseDefinitions.indices) {
                    val notedId = c.getNotedItem(doseDefinitions[i].fourDoseId)
                    if (notedId > 0) {
                        val invAmt = c.getInvAmt(notedId)
                        amount[i] += invAmt * 4L
                        vials[i] += invAmt.toLong()
                        c.deleteItem(notedId, invAmt)
                    }
                }
                for (i in doseDefinitions.indices) {
                    val notedId = c.getNotedItem(doseDefinitions[i].threeDoseId)
                    if (notedId > 0) {
                        val invAmt = c.getInvAmt(notedId)
                        amount[i] += invAmt * 3L
                        vials[i] += invAmt.toLong()
                        c.deleteItem(notedId, invAmt)
                    }
                }
                for (i in doseDefinitions.indices) {
                    val notedId = c.getNotedItem(doseDefinitions[i].twoDoseId)
                    if (notedId > 0) {
                        val invAmt = c.getInvAmt(notedId)
                        amount[i] += invAmt * 2L
                        vials[i] += invAmt.toLong()
                        c.deleteItem(notedId, invAmt)
                    }
                }
                for (i in doseDefinitions.indices) {
                    val notedId = c.getNotedItem(doseDefinitions[i].oneDoseId)
                    if (notedId > 0) {
                        val invAmt = c.getInvAmt(notedId)
                        amount[i] += invAmt.toLong()
                        vials[i] += invAmt.toLong()
                        c.deleteItem(notedId, invAmt)
                    }
                }

                for (i in amount.indices) {
                    val definition = doseDefinitions[i]
                    val id = if (button == 1) definition.fourDoseId else if (button == 2) definition.threeDoseId else if (button == 3) definition.twoDoseId else definition.oneDoseId
                    val divide = if (button == 1) 4 else if (button == 2) 3 else if (button == 3) 2 else 1
                    if (c.getNotedItem(id) > 0) {
                        val invAmt = (amount[i] / divide).toInt()
                        var leftOver = (amount[i] % divide).toInt()
                        val invEmpty = (vials[i] - invAmt - if (leftOver > 0) 1 else 0).toInt()
                        val emptyAmount = c.getInvAmt(230)
                        leftOver = if (leftOver == 3) definition.threeDoseId else if (leftOver == 2) definition.twoDoseId else if (leftOver == 1) definition.oneDoseId else -1

                        if (invAmt > 0 && !c.addItem(c.getNotedItem(id), invAmt)) {
                            Ground.addFloorItem(c, c.getNotedItem(id), invAmt)
                            ItemLog.playerDrop(c, c.getNotedItem(id), invAmt, c.position.copy(), "Decant dropped " + c.getItemName(id).lowercase())
                            c.sendMessage("<col=FF0000>You dropped the " + c.getItemName(id).lowercase() + " to the floor!")
                        }
                        if (invEmpty > 0 && (invEmpty + emptyAmount) < 1000 && !c.addItem(230, invEmpty)) {
                            Ground.addFloorItem(c, 230, invEmpty)
                            ItemLog.playerDrop(c, 230, invEmpty, c.position.copy(), "Decant dropped " + c.getItemName(230).lowercase())
                            c.sendMessage("<col=FF0000>You dropped the " + c.getItemName(230).lowercase() + " to the floor!")
                        } else if (invEmpty < 0) {
                            c.deleteItem(230, invEmpty * -1)
                        }
                        if (leftOver > 0 && !c.addItem(leftOver, 1)) {
                            Ground.addFloorItem(c, leftOver, 1)
                            ItemLog.playerDrop(c, leftOver, 1, c.position.copy(), "Decant dropped " + c.getItemName(leftOver).lowercase())
                            c.sendMessage("<col=FF0000>You dropped the " + c.getItemName(leftOver).lowercase() + " to the floor!")
                        }
                    }
                }
                c.checkItemUpdate()
                DialogueService.showNpcChat(c, npcTalkTo, 591, arrayOf("Enjoy your decanted potions " + c.playerName))
            }
        } else if (dialogueId == 20931) {
            if (button == 1) {
                PyramidPlunder.reset(c)
            }
            c.send(RemoveInterfaces())
        } else if (dialogueId == 48054) {
            if (c.getInvAmt(621) < 1) {
                c.sendMessage("You need a ship ticket to unlock this travel!")
            } else if (button == 1) {
                val id = if (c.actionButtonId == 48054) 4 else if (c.actionButtonId == 3056) 3 else c.actionButtonId - 3058
                if (c.getTravel(id)) {
                    c.sendMessage("You have already unlocked this travel!")
                } else {
                    c.deleteItem(621, 1)
                    c.checkItemUpdate()
                    c.saveTravel(id)
                    c.sendMessage("You have now unlocked the travel!")
                }
            }
            resetDialogue()
            c.setTravelMenu()
        }

        val nextDialogueId = DialogueService.nextDialogueId(c)
        if (nextDialogueId > 0) {
            setDialogue(nextDialogueId)
            setDialogueSent(false)
            setNextDialogue(-1)
        }
    }
}
