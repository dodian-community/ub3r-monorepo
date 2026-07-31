package net.dodian.uber.game.persistence.account.login

import java.util.Date
import net.dodian.uber.game.Server
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Friend
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.engine.systems.world.item.Ground
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.model.player.skills.Skills
import net.dodian.uber.game.engine.systems.skills.prayer.PrayerManager
import net.dodian.uber.game.engine.systems.skills.asSkillPlayer
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.persistence.account.Login
import org.slf4j.LoggerFactory

internal object AccountLoginMapper {
    private val logger = LoggerFactory.getLogger(AccountLoginMapper::class.java)
    private const val DEFAULT_HITPOINTS_XP = 1155
    private const val DEFAULT_HITPOINTS_LEVEL = 10
    private const val DEFAULT_SKILL_XP = 0
    private const val DEFAULT_SKILL_LEVEL = 1

    fun applyExistingCharacter(player: Client, row: AccountLoginRepository.JoinedCharacterRow) {
        applyLook(player, row.look)
        player.latestNews = row.latestNews
        if (isSafePosition(row.x, row.y, row.z)) {
            player.loginPosition(row.x, row.y, row.z)
        } else {
            reject(player, "position", "x=${row.x} y=${row.y} z=${row.z}")
            player.resetPos()
        }

        player.mutedTill = row.unmuteTime
        player.fightType = row.fightStyle
        player.autocast_spellIndex = row.autocast

        val prayerParts = splitOrEmpty(row.prayer, ":")
        val boostedParts = splitOrEmpty(row.boosted, ":")
        val prayerLevel = prayerParts.firstOrNull()?.toIntOrNull() ?: 0

        if (row.statsPresent) {
            applyJoinedStats(player, row.skillExperience, row.health, prayerLevel)
        } else {
            applyDefaultStats(player)
        }

        applyPrayerBoosts(player, prayerParts, boostedParts, row.prayer, row.boosted)
        applyInventory(player, row.inventory)
        applyEquipment(player, row.equipment)
        player.setTask(row.slayerData)
        net.dodian.uber.skills.agility.AgilityModule.restoreProgressFromSave(player.asSkillPlayer(), row.agilityStage)
        player.setTravel(row.travel)
        applyUnlocks(player, row.unlocks)
        player.questProgressState.loadFromJson(row.questData)
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
            val value = look[index].toIntOrNull()
            if (value == null || value !in -1..255) {
                reject(player, "look", "invalid part index=$index value=${look[index]}")
                player.lookNeeded = true
                return
            }
            parts[index] = value
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
                val button = prayerParse[index].toIntOrNull()
                if (button == null) {
                    reject(player, "prayer", "invalid button index=$index")
                    continue
                }
                val prayerButton = PrayerManager.Prayer.forButton(button) ?: continue
                player.prayerManager.togglePrayer(prayerButton)
            }
        }
        if (boosted.isNotEmpty()) {
            player.lastRecover = boostedParse.firstOrNull()?.toIntOrNull() ?: 0
            for (index in 0 until minOf(boostedParse.size - 1, Skill.VALUES.size)) {
                val boost = boostedParse[index + 1].toIntOrNull()
                if (boost == null) {
                    reject(player, "boosted", "invalid boost index=${index + 1}")
                    continue
                }
                player.boost(boost, Skill.getSkill(index))
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
            val slot = parse[0].toIntOrNull() ?: continue
            val id = parse[1].toIntOrNull() ?: continue
            val amount = parse[2].toIntOrNull() ?: continue
            if (slot in player.playerItems.indices && amount > 0 && Server.itemManager.hasDefinition(id)) {
                player.playerItems[slot] = id + 1
                player.playerItemsN[slot] = amount
            } else {
                reject(player, "inventory", "slot=$slot id=$id amount=$amount")
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
            val slot = parse[0].toIntOrNull() ?: continue
            val id = parse[1].toIntOrNull() ?: continue
            val amount = parse[2].toIntOrNull() ?: continue
            if (slot !in player.equipment.indices || amount <= 0 || !Server.itemManager.hasDefinition(id)) {
                reject(player, "equipment", "slot=$slot id=$id amount=$amount")
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

    internal fun applyBank(player: Client, bank: String, size: Int = player.bankSize()) {
        if (bank.isEmpty()) {
            return
        }
        if (player.bankSlotTabs == null || player.bankSlotTabs.size != size) {
            player.bankSlotTabs = IntArray(size)
        }
        for (entry in bank.split(" ")) {
            if (entry == "@ph=1") {
                player.bankPlaceholdersEnabled = true
                continue
            }
            val parse = entry.split("-")
            if (parse.size <= 2) {
                continue
            }
            val slot = parse[0].toIntOrNull() ?: continue
            val rawId = parse[1].toIntOrNull() ?: continue
            val amount = parse[2].toIntOrNull() ?: continue
            val tab = if (parse.size >= 4) parse[3].toIntOrNull()?.coerceIn(0, 9) ?: 0 else 0
            if (amount >= 0 && slot in 0 until size && Server.itemManager.hasDefinition(rawId)) {
                // Tarnish banks never retain notes. Only explicit notes are normalized;
                // placeholders and other linked variants keep their original identity.
                val itemId = Server.itemManager.normalizeForBank(rawId)
                val existing = (0 until size).firstOrNull {
                    player.bankItems[it] - 1 == itemId && player.bankItemsN[it] >= 0
                }
                val destination = existing ?: when {
                    player.bankItems[slot] <= 0 -> slot
                    else -> (0 until size).firstOrNull { player.bankItems[it] <= 0 } ?: continue
                }
                player.bankItems[destination] = itemId + 1
                player.bankItemsN[destination] =
                    if (amount == 0 && existing == null) 0
                    else (player.bankItemsN[destination].toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                if (existing == null) {
                    player.bankSlotTabs[destination] = tab
                }
            } else {
                reject(player, "bank", "slot=$slot id=$rawId amount=$amount")
            }
        }
    }

    private fun applyPouches(player: Client, pouchData: String) {
        if (pouchData.isEmpty()) {
            return
        }
        val pouches = pouchData.split(":")
        for (index in pouches.indices) {
            if (index >= player.runePouchState.size()) {
                break
            }
            val amount = pouches[index].toIntOrNull()
            if (amount == null || amount < 0) {
                reject(player, "rune-pouches", "invalid amount index=$index")
                continue
            }
            player.runePouchState.setAmount(index, amount)
        }
    }

    private fun applySongs(player: Client, songData: String) {
        if (songData.isEmpty()) {
            return
        }
        val songs = songData.split(" ")
        for (index in songs.indices) {
            if (songs[index].isNotEmpty()) {
                val unlocked = songs[index].toIntOrNull()
                if (unlocked == null) {
                    reject(player, "songs", "invalid value index=$index")
                    continue
                }
                player.setSongUnlocked(index, unlocked == 1)
            }
        }
    }

    private fun applyFriends(player: Client, friendData: String) {
        if (friendData.isEmpty()) {
            return
        }
        for (friend in friendData.split(" ")) {
            if (friend.isNotEmpty()) {
                val friendId = friend.toLongOrNull()
                if (friendId == null) {
                    reject(player, "friends", "invalid friend id")
                    continue
                }
                player.friends.add(Friend(friendId, true))
            }
        }
    }

    private fun applyBossLog(player: Client, bossLog: String?) {
        if (bossLog == null) {
            player.bossKillLogState.reset()
            return
        }
        for (line in bossLog.split(" ")) {
            val parts = line.split(":")
            if (parts.size >= 2) {
                val count = parts[1].toIntOrNull()
                if (count == null || count < 0) {
                    reject(player, "boss-log", "invalid count name=${parts[0]}")
                    continue
                }
                player.bossCount(parts[0], count)
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
                val count = parts[1].toIntOrNull()
                if (count == null || count < 0) {
                    reject(player, "monster-log", "invalid count name=${parts[0]}")
                    continue
                }
                player.monsterCount.add(count)
            }
        }
    }

    private fun applyEffects(player: Client, effects: String?) {
        if (effects.isNullOrEmpty()) {
            return
        }
        val lines = effects.split(":")
        for (index in lines.indices.take(MAX_EFFECTS)) {
            val effect = lines[index].toIntOrNull()
            if (effect == null) {
                reject(player, "effects", "invalid value at index=$index")
                continue
            }
            player.effects.add(index, effect)
        }
        if (lines.size > MAX_EFFECTS) reject(player, "effects", "too many entries=${lines.size}")
    }

    private fun applyDailyReward(player: Client, dailyReward: String?) {
        if (dailyReward.isNullOrEmpty()) {
            player.defaultDailyReward(player)
            return
        }
        val today = Date()
        val lines = dailyReward.split(";")
        for (index in lines.indices) {
            val parts = lines[index].split(",")
            val newDay = player.dateDays(Date(parts[0].toLong()), today) > 0
            if (index == 0) {
                if (newDay) {
                    player.dailyReward.add(0, today.time.toString())
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
        try {
            if (farmingData != null && farmingData != "[]") {
                player.farmingJson.farmingLoad(farmingData)
            } else {
                player.farmingJson.farmingLoad("")
            }
        } catch (exception: RuntimeException) {
            reject(player, "farming", exception::class.java.simpleName)
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

    private fun isSafePosition(x: Int, y: Int, z: Int): Boolean = x in 1 until 16_384 && y in 1 until 16_384 && z in 0..3

    private fun reject(player: Client, field: String, detail: String) {
        logger.warn("Persisted player state rejected dbId={} player={} field={} detail={}", player.dbId, player.playerName, field, detail)
    }

    private const val MAX_EFFECTS = 64
}
