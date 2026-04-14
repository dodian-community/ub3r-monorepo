package net.dodian.uber.game.ui

import java.text.DecimalFormat
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendString
import kotlin.jvm.JvmName

enum class QuestTabEntry(
    private val idRaw: Int,
    private val configRaw: Int,
    private val clickIdRaw: Int,
    private val endRaw: Int,
    private val questName: String,
) {
    PLAGUE_DOCKS(0, 7332, 7332, 5, "Mysterium of the Docks"),
    EMPTY_1(1, 7333, -1, 5, ""),
    EMPTY_2(2, 7334, -1, 10, ""),
    EMPTY_3(3, 7336, -1, 10, ""),
    EMPTY_4(4, 7383, -1, 10, ""),
    EMPTY_5(5, 7339, -1, 10, ""),
    EMPTY_6(6, 7338, -1, 10, ""),
    EMPTY_7(7, 7340, -1, 10, ""),
    EMPTY_8(8, 7346, -1, 10, ""),
    EMPTY_9(9, 7341, -1, 10, ""),
    EMPTY_10(10, 7342, -1, 10, ""),
    EMPTY_11(11, 7337, -1, 10, ""),
    EMPTY_12(12, 7343, -1, 10, ""),
    EMPTY_13(13, 7335, -1, 10, ""),
    EMPTY_14(14, 7344, -1, 10, ""),
    EMPTY_15(15, 7345, -1, 10, ""),
    EMPTY_16(16, 7347, -1, 10, ""),
    EMPTY_17(17, 7348, -1, 10, ""),
    ;

    fun getId(): Int = idRaw

    fun getConfig(): Int = configRaw

    fun getClickId(): Int = clickIdRaw

    @get:JvmName("getClickIdKotlin")
    val clickId: Int
        get() = getClickId()

    fun getEnd(): Int = endRaw

    fun getName(): String = questName

    companion object {
        private val QUEST_SEND_VALUES = values()
        private val HOURS_FORMAT = ThreadLocal.withInitial { DecimalFormat("0.000") }
        private var cachedUptimeMinutes = Long.MIN_VALUE
        private var cachedUptimeText = "@gre@[0@gre@] minutes uptime"

        @JvmStatic
        fun getSender(button: Int): QuestTabEntry? {
            for (quest in QUEST_SEND_VALUES) {
                if (quest.getClickId() == button) {
                    return quest
                }
            }
            return null
        }

        @JvmStatic
        fun questInterface(client: Client): QuestTabEntry? {
            client.sendCachedString("Dodian Quests", 640)
            client.sendCachedString("Premium", 663)
            client.sendCachedString("Other Stuff", 682)
            for (quest in QUEST_SEND_VALUES) {
                val stage = client.quests[quest.getId()]
                if (stage == 0) {
                    client.sendCachedString("@red@" + quest.getName(), quest.getConfig())
                }
                if (stage > 0 && stage < quest.getEnd()) {
                    client.sendCachedString("@yel@" + quest.getName(), quest.getConfig())
                }
                if (stage == quest.getEnd()) {
                    client.sendCachedString("@gre@" + quest.getName(), quest.getConfig())
                }
            }
            return null
        }

        @JvmStatic
        fun serverInterface(client: Client): QuestTabEntry? = serverInterface(client, getCachedUptimeText())

        @JvmStatic
        fun serverInterface(client: Client, uptimeText: String): QuestTabEntry? {
            client.sendCachedString("Dodian Server", 640)
            client.sendCachedString("", 682)
            client.sendCachedString(uptimeText, 663)
            client.sendCachedString("@lre@Boss Log", 7332)
            client.sendCachedString("@lre@Monster Log", 7333)
            client.sendCachedString("@lre@Commands", 7334)
            client.sendCachedString("@lre@------------@dre@Links@lre@------------", 7336)
            client.sendCachedString("@gre@News", 7383)
            client.sendCachedString("@gre@Guides", 7339)
            client.sendCachedString("@gre@Account Services", 7338)
            client.sendCachedString("@gre@Discord", 7340)
            if (client.playerRights > 0) {
                client.sendCachedString("@lre@---------@dre@Moderator@lre@---------", 7346)
                client.sendCachedString("@gre@Game CP", 7341)
            }
            return null
        }

        @JvmStatic
        fun questMenu(client: Client, button: Int): Boolean {
            val quest = getSender(button)
            if (client.questPage == 0 && quest != null) {
                client.clearQuestInterface()
                if (quest.getId() == 0) {
                    val stage = client.quests[quest.getId()]
                    client.sendString("@dre@" + quest.getName(), 8144)
                    if (stage == 0) {
                        client.sendString("I can start this quest by talking to the monk in Yanille", 8147)
                        client.sendString("\\nI need to have the following levels:", 8148)
                        client.sendString("", 8149)
                        client.sendString(if (client.getLevel(Skill.HERBLORE) >= 15) "@str@15 herblore@str@" else "15 herblore", 8150)
                        client.sendString(if (client.getLevel(Skill.SMITHING) >= 20) "@str@20 smithing@str@" else "20 smithing", 8151)
                        client.sendString(if (client.getLevel(Skill.CRAFTING) >= 40) "@str@40 crafting@str@" else "40 crafting", 8152)
                    } else if (stage > 0) {
                        client.sendString("@str@I have talked to the monk@str@", 8147)
                        client.sendString("The monk said f' all and I am more confused...", 8148)
                        client.sendString("I wonder why no quest in Dodian exist....", 8149)
                    }
                    client.sendQuestSomething(8143)
                    client.openInterface(8134)
                    return true
                }
            } else {
                when (button) {
                    7332 -> {
                        client.sendString("@dre@Uber Server 3.0 - Boss Log", 8144)
                        client.clearQuestInterface()
                        var line = 8145
                        for (i in client.boss_name.indices) {
                            if (client.boss_amount[i] < 100000) {
                                client.sendString(client.boss_name[i].replace("_", " ") + ": " + client.boss_amount[i], line)
                            } else {
                                client.sendString(client.boss_name[i].replace("_", " ") + ": LOTS", line)
                            }
                            line++
                            if (line == 8196) {
                                line = 12174
                            }
                            if (line == 8146) {
                                line = 8147
                            }
                        }
                        client.sendQuestSomething(8143)
                        client.openInterface(8134)
                        return true
                    }

                    7333 -> {
                        client.sendString("@dre@Uber Server 3.0 - Monster Log", 8144)
                        client.clearQuestInterface()
                        var line = 8145
                        val max = if (client.monsterName.size >= 100) 100 else client.monsterName.size
                        for (i in 0 until max) {
                            val amount = client.monsterCount[i]
                            val name = client.monsterName[i]
                            val newName = name.substring(0, 1).uppercase() + name.substring(1).replace("_", " ")
                            client.sendString(newName + ": " + if (amount == 1048576) "LOTS" else amount.toString(), line)
                            line++
                            if (line == 8196) {
                                line = 12174
                            }
                            if (line == 8146) {
                                line = 8147
                            }
                        }
                        client.sendQuestSomething(8143)
                        client.openInterface(8134)
                        return true
                    }

                    7383 -> {
                        Player.openPage(client, "https://dodian.net/showthread.php?t=" + client.latestNews)
                        return true
                    }

                    7339 -> {
                        Player.openPage(client, "https://dodian.net/forumdisplay.php?f=22")
                        return true
                    }

                    7334 -> {
                        client.sendString("@dre@Uber Server 3.0 - Commands", 8144)
                        client.clearQuestInterface()
                        var line = 8145
                        val commands =
                            arrayOf(
                                "::players or /players \\nSee which player is online.", "",
                                "::droplist, ::drops, /droplist or /drops \\nOpen up the droplist.", "",
                                "::bug or /bug \\nTake you to the forum to post a bug.", "",
                                "::suggest or /suggest \\nTake you to the forum to post a suggestion.", "",
                                "::request or /request \\nTake you to the forum to post a request to a staff.", "",
                                "::report or /report \\nTake you to the forum to post a report to a staff.", "",
                                "::rules or /rules \\nOpen up the rules for the server.", "",
                                "::news or /news \\nOpen up the latest news thread.", "",
                                "::thread id or /thread id \\nOpen up a thread with the id.", "",
                                "::highscore name_One name_Two \\nOpen up the highscore with either one name or compare two.", "",
                                "::price itemname or /price itemname \\nLook up a item's various values.", "",
                                "::max or /max \\nFigure out your maximum combat damage.", "",
                                "::yell msg, /yell msg or //msg \\nWill yell a message to people online.", "",
                                if (client.playerRights > 0) "-------------------Moderator commands ---------------------" else "",
                                if (client.playerRights > 0) "::kick username - Will kick a player with a username." else "",
                                if (client.playerRights > 0) "::toggleyell or ::toggle_yell - Will toggle yell on/off." else "",
                                if (client.playerRights > 0) "::toggletrade or ::toggle_trade - Will toggle trade on/off." else "",
                                if (client.playerRights > 0) "::toggleduel or ::toggle_duel - Will toggle trade on/off." else "",
                                if (client.playerRights > 0) "::togglepvp or ::toggle_pvp - Will toggle pvp on/off." else "",
                                if (client.playerRights > 0) "::toggledrop - Will toggle dropping of items on/off." else "",
                                if (client.playerRights > 0) "::toggleshop or ::toggle_shop - Will toggle shops on/off." else "",
                                if (client.playerRights > 0) "::togglebank or ::toggle_bank - Will toggle banking on/off." else "",
                                if (client.playerRights > 0) "::checkbank username - Will check the bank of username." else "",
                                if (client.playerRights > 0) "::checkinv username - Will check the inventory of username." else "",
                                if (client.playerRights > 0) "::mod msg - Sends a message to all online staff." else "",
                            )
                        for (command in commands) {
                            client.sendString(command, line)
                            line++
                            if (line == 8196) {
                                line = 12174
                            }
                            if (line == 8146) {
                                line = 8147
                            }
                        }
                        client.sendQuestSomething(8143)
                        client.openInterface(8134)
                        return true
                    }

                    7338 -> {
                        Player.openPage(client, "https://dodian.net/forumdisplay.php?f=83")
                        return true
                    }

                    7340 -> {
                        client.discord = true
                        client.showPlayerOption(arrayOf("Are you sure you wish to open discord invite?", "Yes", "No"))
                        return true
                    }

                    7341 -> {
                        Player.openPage(client, "https://dodian.net/index.php?pageid=modcp")
                        return true
                    }
                }
            }
            return false
        }

        @JvmStatic
        fun showMonsterLog(client: Client) {
            val wasPage = client.questPage == 0
            if (wasPage) {
                client.questPage = 1
                questMenu(client, 7333)
                if (wasPage) {
                    client.questPage = 0
                }
            } else {
                questMenu(client, 7333)
            }
        }

        @JvmStatic
        fun clearQuestName(client: Client) {
            for (quest in QUEST_SEND_VALUES) {
                client.sendString("", quest.getConfig())
            }
        }

        @JvmStatic
        fun getCachedUptimeText(): String {
            val uptimeMinutes = (System.currentTimeMillis() - Server.serverStartup) / 60000
            if (uptimeMinutes == cachedUptimeMinutes) {
                return cachedUptimeText
            }

            cachedUptimeMinutes = uptimeMinutes
            cachedUptimeText =
                if (uptimeMinutes < 60) {
                    "@gre@[@whi@$uptimeMinutes@gre@] minutes uptime"
                } else {
                    val hourText =
                        if (uptimeMinutes % 60 == 0L) {
                            (uptimeMinutes / 60).toString()
                        } else {
                            HOURS_FORMAT.get().format(uptimeMinutes / 60.0)
                        }
                    "@gre@[@whi@$hourText@gre@] hours uptime"
                }
            return cachedUptimeText
        }
    }
}
