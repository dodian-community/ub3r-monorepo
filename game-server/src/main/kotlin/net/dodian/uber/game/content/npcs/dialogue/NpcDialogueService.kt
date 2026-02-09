package net.dodian.uber.game.content.npcs.dialogue

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask
import net.dodian.uber.game.netty.listener.out.Frame171
import net.dodian.uber.game.netty.listener.out.NpcDialogueHead
import net.dodian.uber.game.netty.listener.out.PlayerDialogueHead
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendString
import net.dodian.uber.game.netty.listener.out.SetTabInterface
import net.dodian.uber.game.party.RewardItem
import net.dodian.utilities.Misc
import net.dodian.utilities.Utils
import java.text.NumberFormat
import java.util.Objects

object NpcDialogueService {

    @JvmStatic
    fun updateNpcChat(client: Client) = with(client) {
        when (NpcDialogue) {
            1 -> {
                sendFrame200(4883, 591)
                send(SendString(GetNpcName(NpcTalkTo), 4884))
                send(SendString("Good day, how can I help you?", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
            }

            2 -> {
                send(Frame171(1, 2465))
                send(Frame171(0, 2468))
                send(SendString("What would you like to say?", 2460))
                send(SendString("I'd like to access my bank account, please.", 2461))
                send(SendString("I'd like to check my PIN settings.", 2462))
                sendFrame164(2459)
                NpcDialogueSend = true
            }

            3 -> {
                sendFrame200(4883, 591)
                send(SendString(GetNpcName(NpcTalkTo), 4884))
                send(SendString("Do you want to buy some magical gear?", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
                nextDiag = 4
            }

            4 -> {
                send(Frame171(1, 2465))
                send(Frame171(0, 2468))
                send(SendString("Select an Option", 2460))
                send(SendString("Yes please!", 2461))
                send(SendString("Oh it's a rune shop. No thank you, then.", 2462))
                sendFrame164(2459)
                NpcDialogueSend = true
            }

            5 -> {
                sendFrame200(969, 974)
                send(SendString(playerName, 970))
                send(SendString("Oh it's a rune shop. No thank you, then.", 971))
                send(SendString("Click here to continue", 972))
                send(PlayerDialogueHead(969))
                sendFrame164(968)
                NpcDialogueSend = true
            }

            6 -> {
                sendFrame200(4888, 592)
                send(SendString(GetNpcName(NpcTalkTo), 4889))
                send(SendString("Well, if you find somone who does want runes, please", 4890))
                send(SendString("send them my way.", 4891))
                send(SendString("Click here to continue", 4892))
                send(NpcDialogueHead(NpcTalkTo, 4888))
                sendFrame164(4887)
                NpcDialogueSend = true
            }

            7 -> {
                sendFrame200(4883, 591)
                send(SendString(GetNpcName(NpcTalkTo), 4884))
                send(SendString("Well, if you find somone who does want runes, please send them my way.", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
            }

            8 -> {
                sendFrame200(4883, 591)
                send(SendString(GetNpcName(NpcTalkTo), 4884))
                send(SendString("Pins have not been implemented yet", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
            }

            9 -> {
                sendFrame200(4883, 1597)
                send(SendString(GetNpcName(NpcTalkTo), 4884))
                send(SendString("Select an Option", 2460))
                send(SendString("Can you teleport me to the mage arena?", 2461))
                send(SendString("Whats at the mage arena?", 2462))
                sendFrame164(2459)
                NpcDialogueSend = true
            }

            10 -> {
                sendFrame200(4883, 804)
                send(SendString(GetNpcName(804), 4884))
                send(SendString(dMsg, 4885))
                send(NpcDialogueHead(804, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
            }

            17 -> {
                showNpcChat(this, NpcTalkTo, 599, arrayOf("Hello there Traveler.", "Do you fancy taking a carpet ride?", "It will cost 5000 coins."))
                nextDiag = 18
                NpcDialogueSend = true
            }

            18 -> {
                showPlayerChat(this, arrayOf("Where can your carpet take me to?"), 615)
                nextDiag = 19
                NpcDialogueSend = true
            }

            19 -> {
                showNpcChat(this, NpcTalkTo, 598, arrayOf("These are currently the places."))
                nextDiag = 20
                NpcDialogueSend = true
            }

            20 -> {
                val carpetBase = arrayOf("Carpet rides cost 5k coins.", "", "", "", "Cancel")
                val carpetOptions = when (NpcTalkTo) {
                    17 -> arrayOf(carpetBase[0], "Pollnivneach", "Nardah", "Bedabin Camp", carpetBase[4])
                    19 -> arrayOf(carpetBase[0], "Pollnivneach", "Nardah", "Sophanem", carpetBase[4])
                    20 -> arrayOf(carpetBase[0], "Nardah", "Bedabin Camp", "Sophanem", carpetBase[4])
                    22 -> arrayOf(carpetBase[0], "Pollnivneach", "Sophanem", "Bedabin Camp", carpetBase[4])
                    else -> null
                }
                if (carpetOptions != null) {
                    showPlayerOption(this, carpetOptions)
                } else {
                    send(RemoveInterfaces())
                }
                NpcDialogueSend = carpetOptions != null
            }

            1000 -> {
                sendFrame200(4883, npcFace)
                send(SendString(GetNpcName(NpcTalkTo).replace("_", " "), 4884))
                send(SendString("Hi there, what would you like to do?", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
                nextDiag = 1001
            }

            1001 -> {
                send(Frame171(1, 2465))
                send(Frame171(0, 2468))
                send(SendString("What would you like to do?", 2460))
                send(SendString("Gamble", 2461))
                send(SendString("Nothing", 2462))
                sendFrame164(2459)
                NpcDialogueSend = true
            }

            1002 -> {
                sendFrame200(4883, npcFace)
                send(SendString(GetNpcName(NpcTalkTo).replace("_", " "), 4884))
                send(SendString("Cant talk right now!", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
            }

            11 -> {
                sendFrame200(4883, npcFace)
                send(SendString(GetNpcName(NpcTalkTo), 4884))
                send(SendString("Hi there noob, what do you want?", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
                nextDiag = 12
            }

            12 -> {
                if (NpcTalkTo == 405 && (determineCombatLevel() < 50 || getLevel(Skill.SLAYER) < 50)) {
                    sendFrame200(4888, npcFace)
                    send(SendString(GetNpcName(NpcTalkTo), 4889))
                    send(SendString("You need 50 combat and slayer", 4890))
                    send(SendString("to be assign tasks from me!", 4891))
                    send(SendString("Click here to continue", 4892))
                    send(NpcDialogueHead(NpcTalkTo, 4888))
                    sendFrame164(4887)
                } else if (NpcTalkTo == 403 && (!checkItem(989) || getLevel(Skill.SLAYER) < 50)) {
                    sendFrame200(4888, npcFace)
                    send(SendString(GetNpcName(NpcTalkTo), 4889))
                    send(SendString("You need a crystal key and 50 slayer", 4890))
                    send(SendString("to be assign tasks from me!", 4891))
                    send(SendString("Click here to continue", 4892))
                    send(NpcDialogueHead(NpcTalkTo, 4888))
                    sendFrame164(4887)
                } else {
                    val gotHelmet = playerHasItem(11864)
                    val taskName = if (getSlayerData()[0] == -1 || getSlayerData()[3] <= 0) {
                        ""
                    } else {
                        Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData()[1])).textRepresentation
                    }
                    val slayerMaster = arrayOf(
                        "What would you like to say?",
                        "I'd like a task please",
                        if (taskName.isNotEmpty()) "Cancel ${taskName.lowercase()} task" else "No task to skip",
                        "I'd like to upgrade my ${if (gotHelmet) "slayer helmet" else "black mask"}",
                        "Can you teleport me to west ardougne?"
                    )
                    showPlayerOption(this, slayerMaster)
                }
                NpcDialogueSend = true
            }

            13 -> {
                if (NpcTalkTo == 405 && (determineCombatLevel() < 50 || getLevel(Skill.SLAYER) < 50)) {
                    sendFrame200(4888, npcFace)
                    send(SendString(GetNpcName(NpcTalkTo), 4889))
                    send(SendString("You need 50 combat and slayer", 4890))
                    send(SendString("to be assign tasks from me!", 4891))
                    send(SendString("Click here to continue", 4892))
                    send(NpcDialogueHead(NpcTalkTo, 4888))
                    sendFrame164(4887)
                    NpcDialogueSend = true
                    return@with
                }

                if (NpcTalkTo == 403 && (!checkItem(989) || getLevel(Skill.SLAYER) < 50)) {
                    sendFrame200(4888, npcFace)
                    send(SendString(GetNpcName(NpcTalkTo), 4889))
                    send(SendString("You need a crystal key and 50 slayer", 4890))
                    send(SendString("to be assign tasks from me!", 4891))
                    send(SendString("Click here to continue", 4892))
                    send(NpcDialogueHead(NpcTalkTo, 4888))
                    sendFrame164(4887)
                    NpcDialogueSend = true
                    return@with
                }

                val tasks = when (NpcTalkTo) {
                    402 -> SlayerTask.mazchnaTasks(this)
                    403 -> SlayerTask.vannakaTasks(this)
                    405 -> SlayerTask.duradelTasks(this)
                    else -> {
                        send(SendMessage("This npc do not wish to talk to you!"))
                        NpcDialogueSend = true
                        return@with
                    }
                }

                if (tasks.isEmpty()) {
                    send(SendMessage("You cant get any task!"))
                    NpcDialogueSend = true
                    return@with
                }

                if (getSlayerData()[3] > 0) {
                    sendFrame200(4883, npcFace)
                    send(SendString(GetNpcName(NpcTalkTo), 4884))
                    send(SendString("You already have a task!", 4885))
                    send(SendString("Click here to continue", 4886))
                    send(NpcDialogueHead(NpcTalkTo, 4883))
                    sendFrame164(4882)
                    NpcDialogueSend = true
                    return@with
                }

                val task = tasks[Misc.random(tasks.size - 1)]
                val amt = task.assignedAmountRange.value
                sendFrame200(4901, npcFace)
                send(SendString(GetNpcName(NpcTalkTo), 4902))
                send(SendString("You must go out and kill $amt ${task.textRepresentation}", 4903))
                send(SendString("If you want a new task that's too bad", 4904))
                send(SendString("Visit Dodian.net for a slayer guide", 4905))
                send(SendString("", 4906))
                send(SendString("Click here to continue", 4907))
                send(NpcDialogueHead(NpcTalkTo, 4901))
                sendFrame164(4900)
                getSlayerData()[0] = NpcTalkTo
                getSlayerData()[1] = task.ordinal
                getSlayerData()[2] = amt
                getSlayerData()[3] = amt
                NpcDialogueSend = true
            }

            31 -> {
                val taskMonster = if (getSlayerData()[0] == -1 || getSlayerData()[3] <= 0) {
                    ""
                } else {
                    Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData()[1])).textRepresentation
                }
                val skipCost = when (getSlayerData()[0]) {
                    403 -> 250000
                    405 -> 500000
                    else -> 100000
                }

                sendFrame200(4888, npcFace)
                send(SendString(GetNpcName(NpcTalkTo), 4889))
                send(SendString(if (taskMonster.isEmpty()) "You do not have a task currently." else "I can cancel your ${taskMonster.lowercase()} task", 4890))
                send(SendString(if (taskMonster.isEmpty()) "Talk to me to get one." else "task for $skipCost coins.", 4891))
                send(SendString("Click here to continue", 4892))
                send(NpcDialogueHead(NpcTalkTo, 4888))
                sendFrame164(4887)
                if (taskMonster.isNotEmpty()) {
                    nextDiag = 32
                }
                NpcDialogueSend = true
            }

            32 -> {
                val currentTaskName = if (getSlayerData()[0] == -1 || getSlayerData()[3] <= 0) {
                    ""
                } else {
                    Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData()[1])).textRepresentation
                }
                if (currentTaskName.isNotEmpty()) {
                    showPlayerOption(this, arrayOf("Do you wish to cancel your ${currentTaskName.lowercase()} task?", "Yes", "No"))
                }
                NpcDialogueSend = true
            }

            33 -> {
                val taskName = if (getSlayerData()[0] == -1 || getSlayerData()[3] <= 0) {
                    ""
                } else {
                    Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData()[1])).textRepresentation
                }
                val cost = when (getSlayerData()[0]) {
                    403 -> 250000
                    405 -> 500000
                    else -> 100000
                }

                val coinAmount = getInvAmt(995)
                if (coinAmount >= cost) {
                    deleteItem(995, cost)
                    checkItemUpdate()
                    getSlayerData()[3] = 0
                    sendFrame200(4883, npcFace)
                    send(SendString(GetNpcName(NpcTalkTo), 4884))
                    send(SendString("I have now canceled your ${taskName.lowercase()} task!", 4885))
                    send(NpcDialogueHead(NpcTalkTo, 4883))
                    sendFrame164(4882)
                    nextDiag = 12
                } else {
                    sendFrame200(4883, npcFace)
                    send(SendString(GetNpcName(NpcTalkTo), 4884))
                    send(SendString("You do not have enough coins to cancel your task!", 4885))
                    send(NpcDialogueHead(NpcTalkTo, 4883))
                    sendFrame164(4882)
                }
                send(SendString("Click here to continue", 4886))
                NpcDialogueSend = true
            }

            34 -> {
                val slayerHelm = playerHasItem(11864)
                if (!playerHasItem(8921) && !slayerHelm) {
                    showNpcChat(this, NpcTalkTo, 596, arrayOf("You do not have a black mask or a slayer helmet!"))
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("Would you like to upgrade your ${if (slayerHelm) "slayer helmet" else "black mask"}?", "It will cost you 2 million gold pieces."))
                    nextDiag = 35
                }
                NpcDialogueSend = true
            }

            35 -> {
                showPlayerOption(this, arrayOf("Do you wish to upgrade?", "Yes, please", "No, please"))
            }

            36 -> {
                showPlayerChat(this, arrayOf("Yes, thank you."), 614)
                NpcDialogueSend = true
                nextDiag = 37
            }

            37 -> {
                if (!playerHasItem(995, 2000000)) {
                    showNpcChat(this, NpcTalkTo, 596, arrayOf("You do not have enough money!"))
                } else {
                    val gotHelmet = playerHasItem(11864)
                    deleteItem(995, 2000000)
                    deleteItem(if (gotHelmet) 11864 else 8921, 1)
                    addItem(if (gotHelmet) 11865 else 11784, 1)
                    checkItemUpdate()
                    showNpcChat(this, NpcTalkTo, 592, arrayOf("Here is your imbued ${if (gotHelmet) "slayer helmet" else "black mask"}."))
                }
                NpcDialogueSend = true
            }

            14 -> {
                sendFrame200(4883, npcFace)
                send(SendString(GetNpcName(NpcTalkTo), 4884))
                send(SendString("Be careful out there!", 4885))
                send(SendString("Click here to continue", 4886))
                send(NpcDialogueHead(NpcTalkTo, 4883))
                sendFrame164(4882)
                NpcDialogueSend = true
                triggerTele(2542, 3306, 0, false)
            }

            15 -> {
                if (getSlayerData()[0] != -1) {
                    val slayerMaster = getSlayerData()[0]
                    val checkTask = SlayerTask.slayerTasks.getTask(getSlayerData()[1])
                    val lines = if (checkTask != null && getSlayerData()[3] > 0) {
                        arrayOf("You're currently assigned to ${checkTask.textRepresentation};", "you have ${getSlayerData()[3]} more to go.", "Your current streak is ${getSlayerData()[4]}.")
                    } else {
                        arrayOf("You currently have no task.", "Speak to me again to get a new one!")
                    }
                    showNpcChat(this, slayerMaster, npcFace, lines)
                } else {
                    send(SendMessage("You have yet to get a task. Talk to a slayer master!"))
                }
                NpcDialogueSend = true
            }

            16 -> {
                if (getSlayerData().size > 1) {
                    val checkTask = SlayerTask.slayerTasks.getTask(getSlayerData()[1])
                    if (checkTask != null) {
                        showPlayerOption(this, arrayOf("Reset count for ${checkTask.textRepresentation}", "Yes", "No"))
                    } else {
                        showPlayerOption(this, arrayOf("Reset count", "Yes", "No"))
                    }
                } else {
                    showPlayerOption(this, arrayOf("Reset count", "Yes", "No"))
                }
                NpcDialogueSend = true
            }

            21 -> {
                showNpcChat(this, if (gender == 0) 1306 else 1307, 588, arrayOf("Hello there, would you like to change your looks?", "If so, it will be free of charge"))
                NpcDialogueSend = true
            }

            22 -> {
                showPlayerOption(this, arrayOf("Would you like to change your looks?", "Sure", "No thanks"))
                NpcDialogueSend = true
            }

            23 -> {
                showPlayerChat(this, arrayOf("I would love that."), 614)
                NpcDialogueSend = true
            }

            24 -> {
                showPlayerChat(this, arrayOf("Not at the moment."), 614)
                NpcDialogueSend = true
            }

            25 -> {
                send(SetTabInterface(3559, 3213))
                NpcDialogue = 0
                NpcDialogueSend = false
            }

            26 -> {
                showPlayerOption(this, arrayOf("What would you like to do?", "Enable specials", "Disable specials"))
                NpcDialogueSend = true
            }

            27 -> {
                showPlayerOption(this, arrayOf("What would you like to do?", "Enable boss yell messages", "Disable boss yell messages"))
                NpcDialogueSend = true
            }

            162 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("Fancy meeting you here maggot.", "If you have any agility ticket,", "I would gladly take them from you."))
                nextDiag = 163
                NpcDialogueSend = true
            }

            163 -> {
                send(Frame171(1, 2465))
                send(Frame171(0, 2468))
                send(SendString("Trade in tickets or teleport to agility course?", 2460))
                send(SendString("Trade in tickets.", 2461))
                send(SendString("Another course, please.", 2462))
                sendFrame164(2459)
                NpcDialogueSend = true
            }

            164 -> {
                val type = if (skillX == 3002 && skillY == 3931) 3 else if (skillX == 2547 && skillY == 3554) 2 else 1
                val typeGnome = arrayOf("Which course do you wish to be taken to?", "Barbarian", "Wilderness", "Stay here")
                val typeBarbarian = arrayOf("Which course do you wish to be taken to?", "Gnome", "Wilderness", "Stay here")
                val typeWilderness = arrayOf("Which course do you wish to be taken to?", "Gnome", "Barbarian", "Stay here")
                showPlayerOption(this, if (type == 3) typeWilderness else if (type == 2) typeBarbarian else typeGnome)
                NpcDialogueSend = true
            }

            536 -> {
                showPlayerOption(this, arrayOf("Do you wish to enter?", "Sacrifice 5 dragon bones", "Stay here"))
                NpcDialogueSend = true
            }

            1174 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("Hello there adventurer $playerName,", "is there anything you are looking for?"))
                nextDiag = 1175
                NpcDialogueSend = true
            }

            1175 -> {
                showPlayerChat(this, arrayOf("Is there anything you can do for me?", "I heard you know alot about herblore."), 614)
                nextDiag = 1176
                NpcDialogueSend = true
            }

            1176 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("For you $playerName I know the art of decanting.", "I can offer you to decant any noted potions", "that you will bring me, free of charge.", "I also got a herblore store if you wish to take a look."))
                nextDiag = 1177
                NpcDialogueSend = true
            }

            1177 -> {
                showPlayerOption(this, arrayOf("What do you wish to do?", "Visit store", "Decant potions", "Nevermind"))
                NpcDialogueSend = true
            }

            1178 -> {
                showPlayerOption(this, arrayOf("What should we decant it into?", "Four dose", "Three dose", "Two dose", "One dose", "Nevermind"))
                NpcDialogueSend = true
            }

            3837 -> {
                val staffCounting = if (dailyReward.isEmpty()) 0 else Integer.parseInt(dailyReward[2])
                if (staffCounting > 0) {
                    showNpcChat(this, NpcTalkTo, 594, arrayOf("Fancy meeting you here mysterious one.", "I have $staffCounting battlestaffs that", "you can claim for 7000 coins each."))
                    nextDiag = 3838
                } else {
                    showNpcChat(this, NpcTalkTo, 597, arrayOf("Fancy meeting you here mysterious one.", "I have no battlestaffs that you can claim."))
                }
                NpcDialogueSend = true
            }

            3838 -> {
                showPlayerOption(this, arrayOf("Do you wish to claim your battlestaffs?", "Yes", "No"))
                NpcDialogueSend = true
            }

            2345 -> {
                if (!checkUnlock(0)) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("Hello!", "Are you looking to enter my dungeon?", "You have to pay a to enter.", "You can also pay a one time fee."))
                    nextDiag = ++NpcDialogue
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You can enter freely, no need to pay me anything."))
                }
                NpcDialogueSend = true
            }

            2346 -> {
                if (!checkUnlock(0) && checkUnlockPaid(0) != 1) {
                    showPlayerOption(this, arrayOf("Select a option", "Enter fee", "Permanent unlock", "Nevermind"))
                } else if (checkUnlock(0)) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You can enter freely, no need to pay me anything."))
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You have already paid.", "Just enter the dungeon now."))
                }
                NpcDialogueSend = true
            }

            2182, 2347 -> {
                showPlayerOption(this, arrayOf("Select a option", "Ship ticket", "Coins"))
                NpcDialogueSend = true
            }

            2180 -> {
                if (!checkUnlock(1)) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("Hello!", "Are you looking to enter my cave?", "You have to pay a to enter.", "You can also pay a one time fee."))
                    nextDiag = ++NpcDialogue
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You can enter freely, no need to pay me anything."))
                }
                NpcDialogueSend = true
            }

            2181 -> {
                if (!checkUnlock(1) && checkUnlockPaid(1) != 1) {
                    showPlayerOption(this, arrayOf("Select a option", "Enter fee", "Permanent unlock", "Nevermind"))
                } else if (checkUnlock(1)) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You can enter freely, no need to pay me anything."))
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You have already paid.", "Just enter the cave now."))
                }
                NpcDialogueSend = true
            }

            3648 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("Hello dear.", "Would you like to travel?"))
                nextDiag = 3649
            }

            3649 -> {
                showPlayerOption(this, arrayOf("Do you wish to travel?", "Yes", "No"))
            }

            4753 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("Hello ${if (gender == 1) "miss" else "mr"} adventurer.", "What can I help you with today?"))
                nextDiag = 4754
                NpcDialogueSend = true
            }

            4754 -> {
                showPlayerChat(this, arrayOf("I heard you were a famous herbalist.", "I was wondering if you had some kind of service."), 612)
                nextDiag = 4755
                NpcDialogueSend = true
            }

            4755 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("I sure do have some services I can offer.", "Would you like me to make you a unfinish potion or", "Clean any of your herbs? They must all be noted."))
                nextDiag = 4758
                NpcDialogueSend = true
            }

            4756 -> {
                herbMaking = 0
                herbOptions.clear()
                for (h in Utils.grimy_herbs.indices) {
                    if (playerHasItem(GetNotedItem(Utils.grimy_herbs[h]))) {
                        herbOptions.add(RewardItem(GetNotedItem(Utils.grimy_herbs[h]), 0))
                    }
                }
                if (herbOptions.isEmpty()) {
                    showNpcChat(this, 4753, 605, arrayOf("You got no herbs for me to clean!"))
                } else {
                    setHerbOptions()
                }
                NpcDialogueSend = true
            }

            4757 -> {
                herbMaking = 0
                herbOptions.clear()
                for (h in Utils.herbs.indices) {
                    if (playerHasItem(GetNotedItem(Utils.herbs[h]))) {
                        herbOptions.add(RewardItem(GetNotedItem(Utils.herb_unf[h]), 0))
                    }
                }
                if (herbOptions.isEmpty()) {
                    showNpcChat(this, 4753, 605, arrayOf("You got no herbs for me to make into unfinish potions!"))
                } else {
                    setHerbOptions()
                }
                NpcDialogueSend = true
            }

            4758 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("This service will cost you 200 coins per herb", "and 1000 coins per potion.", "I also got a nice store if you wish to take a look."))
                nextDiag = 4759
                NpcDialogueSend = true
            }

            4759 -> {
                showPlayerOption(this, arrayOf("Select a option", "Visit the store", "Clean herbs", "Make unfinish potions"))
                NpcDialogueSend = true
            }

            6481 -> {
                if (totalLevel() >= Skills.maxTotalLevel()) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("I see that you have trained up all your skills.", "I am utmost impressed!"))
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You are quite weak!"))
                }
                nextDiag = NpcDialogue + 1
                NpcDialogueSend = true
            }

            6482 -> {
                if (totalLevel() >= Skills.maxTotalLevel()) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("Would you like to purchase this cape on my back?", "It will cost you 13.37 million coins."))
                    nextDiag = NpcDialogue + 1
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("Come back when you have trained up your skills!"))
                }
                NpcDialogueSend = true
            }

            6483 -> {
                showPlayerOption(this, arrayOf("Purchase the max cape?", "Yes", "No"))
                NpcDialogueSend = true
            }

            6484 -> {
                val coins = 13370000
                val freeSlot = if (getInvAmt(995) == coins) 1 else 2
                if (freeSlots() < freeSlot) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You need atleast ${if (freeSlot == 1) "one" else "two"} free inventory slot${if (freeSlot != 1) "s" else ""}."))
                    nextDiag = NpcTalkTo
                } else if (!playerHasItem(995, coins)) {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("You are missing ${NumberFormat.getNumberInstance().format(coins - getInvAmt(995))} coins!"))
                } else {
                    showNpcChat(this, NpcTalkTo, 591, arrayOf("Here you go.", "Max cape just for you."))
                    deleteItem(995, coins)
                    addItem(13281, 1)
                    addItem(13280, 1)
                    checkItemUpdate()
                }
                NpcDialogueSend = true
            }

            8051 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("Happy Holidays adventurer!"))
                nextDiag = 8052
                NpcDialogueSend = true
            }

            8052 -> {
                showNpcChat(this, NpcTalkTo, 591, arrayOf("The monsters are trying to ruin the new year!", "You must slay them to take back your gifts and", "save the spirit of 2021!"))
                nextDiag = 8053
                NpcDialogueSend = true
            }

            8053 -> {
                showPlayerOption(this, arrayOf("Select a option", "I'd like to see your shop.", "I'll just be on my way."))
                NpcDialogueSend = true
            }

            48054 -> {
                showPlayerOption(this, arrayOf("Unlock the travel?", "Yes", "No"))
                NpcDialogueSend = true
            }

            10000 -> {
                if (getLevel(Skill.SMITHING) >= 60 && playerHasItem(2347)) {
                    showPlayerOption(this, arrayOf("What would you like to make?", "Head", "Body", "Legs", "Boots", "Gloves"))
                } else {
                    send(SendMessage(if (getLevel(Skill.SMITHING) < 60) "You need level 60 smithing to do this." else "You need a hammer to handle this material."))
                    NpcDialogueSend = true
                }
            }

            20931 -> {
                showPlayerOption(this, arrayOf("Exit pyramid plunder?", "Yes", "No"))
                NpcDialogueSend = true
            }
        }
    }

    @JvmStatic
    fun showPlayerOption(client: Client, text: Array<String>) = with(client) {
        var base = 2459
        if (text.size == 4) base = 2469
        if (text.size == 5) base = 2480
        if (text.size == 6) base = 2492

        send(Frame171(1, base + 4 + text.size - 1))
        send(Frame171(0, base + 7 + text.size - 1))
        for (i in text.indices) {
            send(SendString(text[i], base + 1 + i))
        }
        sendFrame164(base)
    }

    @JvmStatic
    fun showNpcChat(client: Client, npcId: Int, emote: Int, text: Array<String>) = with(client) {
        var base = 4882
        if (text.size == 2) base = 4887
        if (text.size == 3) base = 4893
        if (text.size == 4) base = 4900

        sendFrame200(base + 1, emote)
        send(SendString(GetNpcName(npcId), base + 2))
        for (i in text.indices) {
            send(SendString(text[i], base + 3 + i))
        }
        send(SendString("Click here to continue", base + 3 + text.size))
        send(NpcDialogueHead(npcId, base + 1))
        sendFrame164(base)
    }

    @JvmStatic
    fun showPlayerChat(client: Client, text: Array<String>, emote: Int) = with(client) {
        var base = 968
        if (text.size == 2) base = 973
        if (text.size == 3) base = 979
        if (text.size == 4) base = 986

        send(PlayerDialogueHead(base + 1))
        sendFrame200(base + 1, emote)
        send(SendString(playerName, base + 2))
        for (i in text.indices) {
            send(SendString(text[i], base + 3 + i))
        }
        sendFrame164(base)
    }
}
