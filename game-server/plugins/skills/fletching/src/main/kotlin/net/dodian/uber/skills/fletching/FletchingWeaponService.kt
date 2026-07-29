package net.dodian.uber.skills.fletching

/** Fletching-owned fallback weapon classification for cache definitions without a type. */
object FletchingWeaponService {
    @JvmStatic fun isBowWeapon(itemId: Int): Boolean = itemId in bowWeaponIds

    private val bowWeaponIds: Set<Int> by lazy {
        buildSet {
            FletchingModule.bowLogs.forEach { add(it.shortbowId); add(it.longbowId) }
            addAll(listOf(839, 841, 4212, 6724, 20997, 11235, 4734))
            addAll(12765..12768)
        }
    }
}
