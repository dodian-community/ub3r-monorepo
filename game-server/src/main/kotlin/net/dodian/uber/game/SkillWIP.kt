package net.dodian.uber.game

enum class SkillWIP(val id: Int, val compLevel: Int, val compCurrent: Int, val isEnabled: Boolean = false) {
    ATTACK(0, compLevel = 24137, compCurrent = 24138),
    STRENGTH(1, compLevel = 24153, compCurrent = 24154),
    DEFENCE(2, compLevel = 0, compCurrent = 0),
    RANGED(3, compLevel = 0, compCurrent = 0),
    PRAYER(4, compLevel = 0, compCurrent = 0),
    MAGIC(5, compLevel = 0, compCurrent = 0),
    HITPOINTS(6, compLevel = 24139, compCurrent = 24140),
    AGILITY(7, compLevel = 0, compCurrent = 0),
    HERBLORE(8, compLevel = 0, compCurrent = 0),
    THIEVING(9, compLevel = 0, compCurrent = 0),
    CRAFTING(10, compLevel = 0, compCurrent = 0),
    FLETCHING(11, compLevel = 0, compCurrent = 0),
    MINING(12, compLevel = 0, compCurrent = 0),
    SMITHING(13, compLevel = 0, compCurrent = 0),
    FISHING(14, compLevel = 0, compCurrent = 0),
    COOKING(15, compLevel = 0, compCurrent = 0),
    FIREMAKING(16, compLevel = 0, compCurrent = 0),
    WOODCUTTING(17, compLevel = 0, compCurrent = 0),
    RUNECRAFTING(18, compLevel = 0, compCurrent = 0),
    SLAYER(19, compLevel = 0, compCurrent = 0),
    FARMING(id = 20, compLevel = 0, compCurrent = 0, isEnabled = false)
    ;

    companion object {
        fun enabled(): List<SkillWIP> = SkillWIP.values().filter { it.isEnabled }
        fun byId(id: Int): SkillWIP? = SkillWIP.values().singleOrNull { it.id == id }
        fun byName(name: String): SkillWIP? = SkillWIP.values().firstOrNull {
            it.name.lowercase().startsWith(name.lowercase())
                    || it.name.lowercase() == name.lowercase()
                    || it.name.lowercase().contains(name.lowercase())
        }
    }
}

fun skillById(id: Int) = SkillWIP.byId(id)
fun skillByName(name: String) = SkillWIP.byName(name)
fun skillsEnabled() = SkillWIP.enabled().stream()