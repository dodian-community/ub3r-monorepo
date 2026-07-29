package net.dodian.uber.game.persistence.player

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.persistence.player.PlayerSaveRepository
import net.dodian.uber.game.persistence.player.PlayerSaveSnapshot
import net.dodian.uber.game.persistence.db.DbTables

class PlayerSaveSqlRepository(
    private val delegate: PlayerSaveRepository = PlayerSaveRepository(),
) {
    private companion object {
        private val ENABLED_SKILLS: List<Skill> = Skill.values().filter { it.isEnabled() }
    }

    fun saveEnvelope(envelope: PlayerSaveEnvelope) {
        delegate.savePrepared(buildPreparedStatements(envelope))
    }

    fun saveEnvelopesAtomically(envelopes: List<PlayerSaveEnvelope>) {
        require(envelopes.isNotEmpty())
        delegate.savePrepared(
            envelopes.flatMap(::buildPreparedStatements).map { it.copy(expectedUpdateCount = 1) },
        )
    }

    internal fun buildPreparedStatements(envelope: PlayerSaveEnvelope): List<PlayerSaveRepository.Statement> {
        val stats = requireNotNull(envelope.segment<StatsSegmentSnapshot>()) { "Missing STATS segment for save envelope dbId=${envelope.dbId}" }
        val statColumns = listOf("total", "combat") + ENABLED_SKILLS.map { it.name.lowercase() } + "totalxp"
        val statValues = listOf(stats.totalLevel, stats.combatLevel) + stats.skillExperience.toList() + stats.totalXp
        val statements = mutableListOf<PlayerSaveRepository.Statement>()
        statements += PlayerSaveRepository.Statement(
            "UPDATE ${DbTables.GAME_CHARACTERS_STATS} SET ${statColumns.joinToString(" = ?, ")} = ? WHERE uid = ?",
            statValues + envelope.dbId,
        )
        if (envelope.updateProgress) {
            statements += PlayerSaveRepository.Statement(
                "INSERT INTO ${DbTables.GAME_CHARACTERS_STATS_PROGRESS} (${(listOf("updated", "uid") + statColumns).joinToString(", ")}) VALUES (${List(statColumns.size + 2) { "?" }.joinToString(", ")})",
                listOf(java.sql.Timestamp(envelope.createdAt), envelope.dbId) + statValues,
            )
        }

        val values = linkedMapOf<String, Any?>(
            "pkrating" to 1500,
            "lastlogin" to envelope.createdAt,
            "health" to stats.currentHealth,
            "fightStyle" to stats.fightType,
            "prayer" to encodePrayer(stats),
            "boosted" to encodeBoosted(stats),
        )
        envelope.segment<EquipmentSegmentSnapshot>()?.let { values["equipment"] = encodeItemSlots(it.entries) }
        envelope.segment<InventorySegmentSnapshot>()?.let { values["inventory"] = encodeItemSlots(it.entries) }
        envelope.segment<BankSegmentSnapshot>()?.let { values["bank"] = encodeBank(it) }
        envelope.segment<SocialSegmentSnapshot>()?.let { values["friends"] = it.friends.joinToString(" ") }
        envelope.segment<SlayerSegmentSnapshot>()?.let {
            values["slayerData"] = it.slayerData
            values["essence_pouch"] = it.essencePouch
            values["autocast"] = it.autocastSpellIndex
        }
        envelope.segment<EffectsSegmentSnapshot>()?.let { values["effects"] = it.effects.joinToString(":") }
        envelope.segment<PositionSegmentSnapshot>()?.let {
            values["height"] = it.height; values["x"] = it.x; values["y"] = it.y
        }
        envelope.segment<FarmingSegmentSnapshot>()?.let {
            values["farming"] = it.farming; values["dailyReward"] = encodeDailyReward(it.dailyReward)
        }
        envelope.segment<LooksSegmentSnapshot>()?.let {
            values["songUnlocked"] = it.songUnlocked; values["travel"] = it.travel
            values["look"] = it.look; values["unlocks"] = it.unlocks
        }
        envelope.segment<MetaSegmentSnapshot>()?.let {
            values["news"] = it.latestNews; values["agility"] = it.agilityCourseStage
            values["Monster_Log"] = encodeNamedCounts(it.monsterLog, ",", ";")
            values["Boss_Log"] = encodeNamedCounts(it.bossLog, ":", " ")
        }
        statements += PlayerSaveRepository.Statement(
            "UPDATE ${DbTables.GAME_CHARACTERS} SET ${values.keys.joinToString(" = ?, ")} = ? WHERE id = ?",
            values.values.toList() + envelope.dbId,
        )
        return statements
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
                setRaw("bank='${encodeBank(bank)}'")
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
        entries.joinToString(" ") { entry ->
            if (entry.tab != 0) "${entry.slot}-${entry.itemId}-${entry.amount}-${entry.tab}"
            else "${entry.slot}-${entry.itemId}-${entry.amount}"
        }

    /** Backward-compatible metadata in the existing bank text column; old readers ignore this token. */
    private fun encodeBank(bank: BankSegmentSnapshot): String =
        buildList {
            addAll(bank.entries.map { entry ->
                if (entry.tab != 0) "${entry.slot}-${entry.itemId}-${entry.amount}-${entry.tab}"
                else "${entry.slot}-${entry.itemId}-${entry.amount}"
            })
            if (bank.placeholdersEnabled) add("@ph=1")
        }.joinToString(" ")

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
