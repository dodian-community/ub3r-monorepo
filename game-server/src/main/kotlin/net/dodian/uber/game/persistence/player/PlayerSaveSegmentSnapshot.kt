package net.dodian.uber.game.persistence.player

sealed interface PlayerSaveSegmentSnapshot {
    val segment: PlayerSaveSegment
}

data class StatsSegmentSnapshot(
    val totalLevel: Int,
    val combatLevel: Int,
    val totalXp: Int,
    val skillExperience: IntArray,
    val currentHealth: Int,
    val currentPrayer: Int,
    val prayerButtons: IntArray,
    val boostedLevels: IntArray,
    val lastRecover: Int,
    val fightType: Int,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.STATS
}

data class InventorySegmentSnapshot(
    val entries: List<ItemSlotEntry>,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.INVENTORY
}

data class EquipmentSegmentSnapshot(
    val entries: List<ItemSlotEntry>,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.EQUIPMENT
}

data class BankSegmentSnapshot(
    val entries: List<ItemSlotEntry>,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.BANK
}

data class PositionSegmentSnapshot(
    val x: Int,
    val y: Int,
    val height: Int,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.POSITION
}

data class SocialSegmentSnapshot(
    val friends: List<Long>,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.SOCIAL
}

data class SlayerSegmentSnapshot(
    val slayerData: String,
    val essencePouch: String,
    val autocastSpellIndex: Int,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.SLAYER
}

data class FarmingSegmentSnapshot(
    val farming: String,
    val dailyReward: List<String>,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.FARMING
}

data class EffectsSegmentSnapshot(
    val effects: List<Int>,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.EFFECTS
}

data class LooksSegmentSnapshot(
    val songUnlocked: String,
    val travel: String,
    val look: String,
    val unlocks: String,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.LOOKS
}

data class MetaSegmentSnapshot(
    val latestNews: Int,
    val agilityCourseStage: Int,
    val bossLog: List<NamedCountEntry>,
    val monsterLog: List<NamedCountEntry>,
    val loginDurationMs: Long,
) : PlayerSaveSegmentSnapshot {
    override val segment: PlayerSaveSegment = PlayerSaveSegment.META
}

