package net.dodian.uber.game.model.entity.player

/** Persisted kill counters for the fixed legacy boss-log list. */
class BossKillLogState {
    private val counts = IntArray(NAMES.size)

    fun size(): Int = NAMES.size
    fun nameAt(index: Int): String = NAMES[index]
    fun countAt(index: Int): Int = counts[index]
    fun reset() = counts.fill(0)
    fun set(name: String, count: Int) { indexOf(name)?.let { counts[it] = count } }
    fun incrementForNpcName(npcName: String) { indexOfNpcName(npcName)?.let { if (counts[it] < MAX_DISPLAYED_COUNT) counts[it]++ } }
    fun countForNpcName(npcName: String): Int = indexOfNpcName(npcName)?.let(counts::get) ?: 0

    private fun indexOf(name: String) = NAMES.indexOfFirst { it.equals(name, true) }.takeIf { it >= 0 }
    private fun indexOfNpcName(npcName: String) = NAMES.indexOfFirst { npcName.equals(it.replace("_", " "), true) }.takeIf { it >= 0 }

    companion object {
        const val MAX_DISPLAYED_COUNT = 100_000
        @JvmField val NAMES = arrayOf(
            "Dad", "Abyssal_Guardian", "San_Tojalon", "Black_Knight_Titan", "Jungle_Demon", "Ungadulu", "Nechryael", "Ice_Queen",
            "King_Black_Dragon", "Head_Mourners", "Black_Demon", "Dagannoth_Prime", "Dwayne", "TzTok-Jad", "Kalphite_queen", "Kalphite_king", "Venenatis",
        )
    }
}
