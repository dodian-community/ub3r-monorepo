package net.dodian.uber.game.persistence.player

enum class PlayerSaveSegment(val mask: Int) {
    STATS(1 shl 0),
    PROGRESS(1 shl 1),
    INVENTORY(1 shl 2),
    EQUIPMENT(1 shl 3),
    BANK(1 shl 4),
    POSITION(1 shl 5),
    SOCIAL(1 shl 6),
    SLAYER(1 shl 7),
    FARMING(1 shl 8),
    EFFECTS(1 shl 9),
    LOOKS(1 shl 10),
    META(1 shl 11);

    companion object {
        @JvmField
        val ALL_MASK: Int = values().fold(0) { acc, segment -> acc or segment.mask }

        @JvmStatic
        fun fromMask(mask: Int): List<PlayerSaveSegment> =
            values().filter { segment -> mask and segment.mask != 0 }
    }
}
