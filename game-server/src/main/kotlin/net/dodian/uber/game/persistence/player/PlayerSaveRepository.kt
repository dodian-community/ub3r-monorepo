package net.dodian.uber.game.persistence.player

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.persistence.PlayerSaveRepository
import net.dodian.uber.game.persistence.PlayerSaveSnapshot
import net.dodian.utilities.DbTables

class PlayerSaveRepository(
    private val delegate: PlayerSaveRepository = PlayerSaveRepository(),
) {
    private companion object {
        private val ENABLED_SKILLS: List<Skill> = Skill.values().filter { it.isEnabled }
    }

    fun saveEnvelope(envelope: PlayerSaveEnvelope) {
        delegate.saveSnapshot(buildSnapshot(envelope))
    }

    fun buildSnapshot(envelope: PlayerSaveEnvelope): PlayerSaveSnapshot {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(envelope.createdAt))
        val stats = envelope.segment<StatsSegmentSnapshot>()
        val inventory = envelope.segment<InventorySegmentSnapshot>()
        val bank = envelope.segment<BankSegmentSnapshot>()
        val equipment = envelope.segment<EquipmentSegmentSnapshot>()
        val position = envelope.segment<PositionSegmentSnapshot>()
        val social = envelope.segment<SocialSegmentSnapshot>()
        val slayer = envelope.segment<SlayerSegmentSnapshot>()
        val farming = envelope.segment<FarmingSegmentSnapshot>()
        val effects = envelope.segment<EffectsSegmentSnapshot>()
        val looks = envelope.segment<LooksSegmentSnapshot>()
        val meta = envelope.segment<MetaSegmentSnapshot>()

        val requiredStats = requireNotNull(stats) { "Missing STATS segment for save envelope dbId=${envelope.dbId}" }
        val statsQuery =
            buildString {
                append("UPDATE ")
                append(DbTables.GAME_CHARACTERS_STATS)
                append(" SET total=")
                append(requiredStats.totalLevel)
                append(", combat=")
                append(requiredStats.combatLevel)
                append(", ")
                ENABLED_SKILLS.forEachIndexed { index, skill ->
                    append(skill.name.lowercase())
                    append("=")
                    append(requiredStats.skillExperience[index])
                    append(", ")
                }
                append("totalxp=")
                append(requiredStats.totalXp)
                append(" WHERE uid=")
                append(envelope.dbId)
            }

        val progressQuery =
            if (!envelope.updateProgress) {
                null
            } else {
                buildString {
                    append("INSERT INTO ")
                    append(DbTables.GAME_CHARACTERS_STATS_PROGRESS)
                    append(" SET updated='")
                    append(timestamp)
                    append("', total=")
                    append(requiredStats.totalLevel)
                    append(", combat=")
                    append(requiredStats.combatLevel)
                    append(", uid=")
                    append(envelope.dbId)
                    append(", ")
                    ENABLED_SKILLS.forEachIndexed { index, skill ->
                        append(skill.name.lowercase())
                        append("=")
                        append(requiredStats.skillExperience[index])
                        append(", ")
                    }
                    append("totalxp=")
                    append(requiredStats.totalXp)
                }
            }

        val characterQuery =
            buildCharacterQuery(envelope, requiredStats, inventory, bank, equipment, position, social, slayer, farming, effects, looks, meta)

        return PlayerSaveSnapshot.forSql(
            envelope.sequence,
            envelope.dbId,
            envelope.playerName,
            envelope.reason,
            envelope.updateProgress,
            envelope.finalSave,
            statsQuery,
            progressQuery,
            characterQuery,
        )
    }

    private fun buildCharacterQuery(
        envelope: PlayerSaveEnvelope,
        stats: StatsSegmentSnapshot,
        inventory: InventorySegmentSnapshot?,
        bank: BankSegmentSnapshot?,
        equipment: EquipmentSegmentSnapshot?,
        position: PositionSegmentSnapshot?,
        social: SocialSegmentSnapshot?,
        slayer: SlayerSegmentSnapshot?,
        farming: FarmingSegmentSnapshot?,
        effects: EffectsSegmentSnapshot?,
        looks: LooksSegmentSnapshot?,
        meta: MetaSegmentSnapshot?,
    ): String {
        return buildString {
            append("UPDATE ")
            append(DbTables.GAME_CHARACTERS)
            append(" SET ")

            var first = true
            fun setRaw(fragment: String) {
                if (!first) append(", ") else first = false
                append(fragment)
            }

            setRaw("pkrating=1500")
            setRaw("lastlogin='${System.currentTimeMillis()}'")

            setRaw("health=${stats.currentHealth}")
            setRaw("fightStyle=${stats.fightType}")
            setRaw("prayer='${encodePrayer(stats)}'")
            setRaw("boosted='${encodeBoosted(stats)}'")
            if (equipment != null) {
                setRaw("equipment='${encodeItemSlots(equipment.entries)}'")
            }
            if (inventory != null) {
                setRaw("inventory='${encodeItemSlots(inventory.entries)}'")
            }
            if (bank != null) {
                setRaw("bank='${encodeItemSlots(bank.entries)}'")
            }
            if (social != null) {
                val friendsValue = social.friends.joinToString(" ")
                setRaw("friends='$friendsValue'")
            }
            if (slayer != null) {
                setRaw("slayerData='${slayer.slayerData}'")
                setRaw("essence_pouch='${slayer.essencePouch}'")
                setRaw("autocast=${slayer.autocastSpellIndex}")
            }
            if (effects != null) {
                val effectsValue = effects.effects.joinToString(":")
                setRaw("effects='$effectsValue'")
            }
            if (position != null) {
                setRaw("height=${position.height}")
                setRaw("x=${position.x}")
                setRaw("y=${position.y}")
            }
            if (farming != null) {
                setRaw("farming='${farming.farming}'")
                setRaw("dailyReward='${encodeDailyReward(farming.dailyReward)}'")
            }
            if (looks != null) {
                setRaw("songUnlocked='${looks.songUnlocked}'")
                setRaw("travel='${looks.travel}'")
                setRaw("look='${looks.look}'")
                setRaw("unlocks='${looks.unlocks}'")
            }
            if (meta != null) {
                setRaw("news=${meta.latestNews}")
                setRaw("agility='${meta.agilityCourseStage}'")
                val monsterLogValue = encodeNamedCounts(meta.monsterLog, ",", ";")
                val bossLogValue = encodeNamedCounts(meta.bossLog, ":", " ")
                setRaw("Monster_Log='$monsterLogValue'")
                setRaw("Boss_Log='$bossLogValue'")
            }

            append(" WHERE id = ")
            append(envelope.dbId)
        }
    }

    private fun encodeItemSlots(entries: List<ItemSlotEntry>): String =
        entries.joinToString(" ") { entry -> "${entry.slot}-${entry.itemId}-${entry.amount}" }

    private fun encodeNamedCounts(entries: List<NamedCountEntry>, kvSeparator: String, entrySeparator: String): String =
        entries.joinToString(entrySeparator) { entry -> "${entry.name}$kvSeparator${entry.count}" }

    private fun encodeDailyReward(rewards: List<String>): String {
        if (rewards.isEmpty()) {
            return ""
        }
        return rewards.joinToString(",")
    }

    private fun encodePrayer(stats: StatsSegmentSnapshot): String {
        if (stats.prayerButtons.isEmpty()) {
            return stats.currentPrayer.toString()
        }
        return buildString {
            append(stats.currentPrayer)
            stats.prayerButtons.forEach { buttonId ->
                append(":")
                append(buttonId)
            }
        }
    }

    private fun encodeBoosted(stats: StatsSegmentSnapshot): String =
        buildString {
            append(stats.lastRecover)
            stats.boostedLevels.forEach { boost ->
                append(":")
                append(boost)
            }
        }

    private inline fun <reified T : PlayerSaveSegmentSnapshot> PlayerSaveEnvelope.segment(): T? =
        segments.firstOrNull { it is T } as? T
}
