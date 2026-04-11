package net.dodian.uber.game.persistence.account.login

import java.util.Date
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Friend
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.model.player.skills.prayer.Prayers
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.persistence.account.Login

internal object AccountLoginMapper {
    private const val DEFAULT_HITPOINTS_XP = 1155
    private const val DEFAULT_HITPOINTS_LEVEL = 10
    private const val DEFAULT_SKILL_XP = 0
    private const val DEFAULT_SKILL_LEVEL = 1

    fun applyExistingCharacter(player: Client, row: AccountLoginRepository.JoinedCharacterRow) {
        applyLook(player, row.look)
        player.latestNews = row.latestNews
        player.loginPosition(row.x, row.y, row.z)
        if (row.x < 1 || row.y < 1) {
            player.resetPos()
        }

        player.mutedTill = row.unmuteTime
        player.fightType = row.fightStyle
        player.autocast_spellIndex = row.autocast

        val prayerParts = splitOrEmpty(row.prayer, ":")
        val boostedParts = splitOrEmpty(row.boosted, ":")
        val prayerLevel = if (row.prayer.isEmpty()) 0 else prayerParts[0].toInt()

        if (row.statsPresent) {
            applyJoinedStats(player, row.skillExperience, row.health, prayerLevel)
        } else {
            applyDefaultStats(player)
        }

        applyPrayerBoosts(player, prayerParts, boostedParts, row.prayer, row.boosted)
        applyInventory(player, row.inventory)
        applyEquipment(player, row.equipment)
        player.setTask(row.slayerData)
        player.agilityCourseStage = row.agilityStage
        player.setTravel(row.travel)
        applyUnlocks(player, row.unlocks)
        applyBank(player, row.bank)
        applyPouches(player, row.essencePouch)
        applySongs(player, row.songUnlocked)
        applyFriends(player, row.friends)
        applyBossLog(player, row.bossLog)
        applyMonsterLog(player, row.monsterLog)
        applyEffects(player, row.effects)
        applyDailyReward(player, row.dailyReward)
        applyFarming(player, row.farming)

        if (row.lastLogin == 0L) {
            giveDefaultStarterItems(player)
        }
    }

    fun applyNewCharacterDefaults(player: Client) {
        player.lookNeeded = true
        player.resetPos()
        applyDefaultStats(player)
        giveDefaultStarterItems(player)
        player.defaultDailyReward(player)
        player.farmingJson.farmingLoad("")
    }

    fun applyDefaultStats(player: Client) {
        for (skill in Skill.enabledSkills()) {
            val experience = if (skill == Skill.HITPOINTS) DEFAULT_HITPOINTS_XP else DEFAULT_SKILL_XP
            val level = if (skill == Skill.HITPOINTS) DEFAULT_HITPOINTS_LEVEL else DEFAULT_SKILL_LEVEL
            player.setExperience(experience, skill)
            player.setLevel(level, skill)
        }
        player.maxHealth = DEFAULT_HITPOINTS_LEVEL
        player.currentHealth = DEFAULT_HITPOINTS_LEVEL
        player.maxPrayer = DEFAULT_SKILL_LEVEL
        player.currentPrayer = DEFAULT_SKILL_LEVEL
    }

    private fun applyLook(player: Client, lookData: String) {
        if (lookData.isEmpty()) {
            player.lookNeeded = true
            return
        }

        val look = lookData.split(" ")
        if (look.size != 13) {
            player.lookNeeded = true
            return
        }

        val parts = IntArray(13)
        for (index in look.indices) {
            parts[index] = look[index].toInt()
        }
        player.setLook(parts)
    }

    private fun applyJoinedStats(player: Client, skillExperience: Map<Skill, Int>, health: Int, prayerLevel: Int) {
        for (skill in Skill.enabledSkills()) {
            val experience = skillExperience[skill] ?: 0
            player.setExperience(experience, skill)
            player.setLevel(Skills.getLevelForExperience(experience), skill)
            if (skill == Skill.HITPOINTS) {
                player.maxHealth = Skills.getLevelForExperience(experience)
                player.currentHealth = if (health < 1 || health > player.maxHealth) player.maxHealth else health
            } else if (skill == Skill.PRAYER) {
                player.maxPrayer = Skills.getLevelForExperience(experience)
                player.currentPrayer = if (prayerLevel < 0 || prayerLevel > player.maxPrayer) player.maxPrayer else prayerLevel
            }
        }
    }

    private fun applyPrayerBoosts(player: Client, prayerParse: Array<String>, boostedParse: Array<String>, prayer: String, boosted: String) {
        if (prayer.isNotEmpty()) {
            for (index in 1 until prayerParse.size) {
                val prayerButton = Prayers.Prayer.forButton(prayerParse[index].toInt()) ?: continue
                player.prayerManager.togglePrayer(prayerButton)
            }
        }
        if (boosted.isNotEmpty()) {
            player.lastRecover = boostedParse[0].toInt()
            for (index in 0 until boostedParse.size - 1) {
                player.boost(boostedParse[index + 1].toInt(), Skill.getSkill(index))
            }
        }
    }

    private fun applyInventory(player: Client, inventory: String) {
        if (inventory.isEmpty()) {
            return
        }
        for (entry in inventory.split(" ")) {
            val parse = entry.split("-")
            if (parse.size <= 2) {
                continue
            }
            val slot = parse[0].toInt()
            val id = parse[1].toInt()
            val amount = parse[2].toInt()
            if (id < 66000) {
                player.playerItems[slot] = id + 1
                player.playerItemsN[slot] = amount
            }
        }
    }

    private fun applyEquipment(player: Client, equipment: String) {
        if (equipment.isEmpty()) {
            return
        }
        for (entry in equipment.split(" ")) {
            val parse = entry.split("-")
            if (parse.size <= 2) {
                continue
            }
            val slot = parse[0].toInt()
            val id = parse[1].toInt()
            val amount = parse[2].toInt()
            if (id > 24000) {
                continue
            }
            if (player.checkEquip(id, slot, -1)) {
                player.equipment[slot] = id
                player.equipmentN[slot] = amount
            } else if (player.freeSlots() == 0 || !player.addItem(id, amount)) {
                Ground.addFloorItem(player, id, amount)
                ItemLog.playerDrop(player, id, amount, player.position.copy(), "Equipment check drop")
                player.sendMessage("<col=FF0000>You dropped the ${Server.itemManager.getName(id).lowercase()} on the floor!!!")
            }
        }
    }

    private fun applyUnlocks(player: Client, unlocks: String) {
        val parsed = if (unlocks.isEmpty()) emptyList() else unlocks.split(":")
        for (index in 0 until player.unlockLength) {
            if (index < parsed.size) {
                player.addUnlocks(index, *parsed[index].split(",").toTypedArray())
            } else {
                player.addUnlocks(index, "0", "0")
            }
        }
    }

    private fun applyBank(player: Client, bank: String) {
        if (bank.isEmpty()) {
            return
        }
        for (entry in bank.split(" ")) {
            val parse = entry.split("-")
            if (parse.size <= 2) {
                continue
            }
            val slot = parse[0].toInt()
            val id = parse[1].toInt()
            val amount = parse[2].toInt()
            if (id < 66600) {
                player.bankItems[slot] = id + 1
                player.bankItemsN[slot] = amount
            }
        }
    }

    private fun applyPouches(player: Client, pouchData: String) {
        if (pouchData.isEmpty()) {
            return
        }
        val pouches = pouchData.split(":")
        for (index in pouches.indices) {
            if (index >= player.runePouchesAmount.size) {
                break
            }
            player.runePouchesAmount[index] = pouches[index].toInt()
        }
    }

    private fun applySongs(player: Client, songData: String) {
        if (songData.isEmpty()) {
            return
        }
        val songs = songData.split(" ")
        for (index in songs.indices) {
            if (songs[index].isNotEmpty()) {
                player.setSongUnlocked(index, songs[index].toInt() == 1)
            }
        }
    }

    private fun applyFriends(player: Client, friendData: String) {
        if (friendData.isEmpty()) {
            return
        }
        for (friend in friendData.split(" ")) {
            if (friend.isNotEmpty()) {
                player.friends.add(Friend(friend.toLong(), true))
            }
        }
    }

    private fun applyBossLog(player: Client, bossLog: String?) {
        if (bossLog == null) {
            for (index in player.boss_name.indices) {
                player.boss_amount[index] = 0
            }
            return
        }
        for (line in bossLog.split(" ")) {
            val parts = line.split(":")
            if (parts.size >= 2) {
                player.bossCount(parts[0], parts[1].toInt())
            }
        }
    }

    private fun applyMonsterLog(player: Client, monsterLog: String?) {
        if (monsterLog.isNullOrEmpty()) {
            return
        }
        for (line in monsterLog.split(";")) {
            val parts = line.split(",")
            if (parts.size == 2) {
                player.monsterName.add(parts[0])
                player.monsterCount.add(parts[1].toInt())
            }
        }
    }

    private fun applyEffects(player: Client, effects: String?) {
        if (effects.isNullOrEmpty()) {
            return
        }
        val lines = effects.split(":")
        for (index in lines.indices) {
            player.effects.add(index, lines[index].toInt())
        }
    }

    private fun applyDailyReward(player: Client, dailyReward: String?) {
        if (dailyReward.isNullOrEmpty()) {
            player.defaultDailyReward(player)
            return
        }
        val lines = dailyReward.split(";")
        for (index in lines.indices) {
            val parts = lines[index].split(",")
            val newDay = player.dateDays(Date(parts[0].toLong()), player.today) > 0
            if (index == 0) {
                if (newDay) {
                    player.dailyReward.add(0, player.today.time.toString())
                    player.dailyReward.add(1, "6000")
                    player.dailyReward.add(2, parts[2])
                    player.dailyReward.add(3, "0")
                    player.dailyReward.add(4, parts[4])
                } else {
                    for (partIndex in parts.indices) {
                        player.dailyReward.add(partIndex, parts[partIndex])
                    }
                }
            } else {
                for (partIndex in parts.indices) {
                    player.dailyReward.add(player.staffSize + partIndex, parts[partIndex])
                }
            }
        }
    }

    private fun applyFarming(player: Client, farmingData: String?) {
        if (farmingData != null && farmingData != "[]") {
            player.farmingJson.farmingLoad(farmingData)
        } else {
            player.farmingJson.farmingLoad("")
        }
    }

    private fun giveDefaultStarterItems(player: Client) {
        player.equipment[Equipment.Slot.WEAPON.id] = 1277
        player.equipment[Equipment.Slot.SHIELD.id] = 1171
        player.equipmentN[Equipment.Slot.WEAPON.id] = 1
        player.equipmentN[Equipment.Slot.SHIELD.id] = 1
        player.addItem(995, 2000)
        player.addItem(1856, 1)
        player.addItem(4155, 1)
        player.checkItemUpdate()
    }

    private fun splitOrEmpty(value: String, delimiter: String): Array<String> =
        if (value.isEmpty()) emptyArray() else value.split(delimiter).toTypedArray()
}
