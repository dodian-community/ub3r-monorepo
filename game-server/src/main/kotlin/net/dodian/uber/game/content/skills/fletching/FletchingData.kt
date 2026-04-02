package net.dodian.uber.game.content.skills.fletching

import net.dodian.uber.game.systems.skills.SkillActionRequest
import net.dodian.uber.game.systems.skills.SkillActionState

data class FletchingLogDefinition(
    val logItemId: Int,
    val unstrungShortbowId: Int,
    val unstrungLongbowId: Int,
    val shortbowId: Int,
    val longbowId: Int,
    val shortLevelRequired: Int,
    val longLevelRequired: Int,
    val shortExperience: Int,
    val longExperience: Int,
    val shortStringAnimationId: Int,
    val longStringAnimationId: Int,
)

data class ArrowRecipe(
    val headId: Int,
    val arrowId: Int,
    val requiredLevel: Int,
    val experience: Int,
)

data class DartRecipe(
    val tipId: Int,
    val dartId: Int,
    val requiredLevel: Int,
    val experience: Int,
)

data class FletchingRequest(
    val logIndex: Int,
    val productId: Int,
    val experience: Int,
    val amount: Int,
) : SkillActionRequest

data class FletchingState(
    val logIndex: Int,
    val productId: Int = -1,
    val experience: Int = 0,
    val remaining: Int = 0,
) : SkillActionState {
    val isActive: Boolean
        get() = productId > 0 && remaining > 0
}

object FletchingData {
    @JvmField
    val bowLogs: List<FletchingLogDefinition> = listOf(
        FletchingLogDefinition(1521, 54, 56, 843, 845, 20, 25, 102, 150, 6679, 6685),
        FletchingLogDefinition(1519, 60, 58, 849, 847, 35, 40, 198, 252, 6680, 6686),
        FletchingLogDefinition(1517, 64, 62, 853, 851, 50, 55, 300, 348, 6681, 6687),
        FletchingLogDefinition(1515, 68, 66, 857, 855, 65, 70, 408, 450, 6682, 6688),
        FletchingLogDefinition(1513, 72, 70, 861, 859, 80, 85, 504, 552, 6683, 6689),
    )

    @JvmField
    val arrowRecipes: List<ArrowRecipe> = listOf(
        ArrowRecipe(39, 882, 1, 7),
        ArrowRecipe(40, 884, 15, 13),
        ArrowRecipe(41, 886, 30, 25),
        ArrowRecipe(42, 888, 45, 38),
        ArrowRecipe(43, 890, 60, 50),
        ArrowRecipe(44, 892, 75, 63),
        ArrowRecipe(11237, 11212, 90, 75),
    )

    @JvmField
    val dartRecipes: List<DartRecipe> = listOf(
        DartRecipe(819, 806, 1, 18),
        DartRecipe(820, 807, 22, 38),
        DartRecipe(821, 808, 37, 75),
        DartRecipe(822, 809, 52, 112),
        DartRecipe(823, 810, 67, 150),
        DartRecipe(824, 811, 81, 188),
    )

    private val bowWeaponIds: Set<Int> =
        buildSet {
            bowLogs.forEach { bow ->
                add(bow.shortbowId)
                add(bow.longbowId)
            }
            addAll(listOf(839, 841, 4212, 6724, 20997, 11235, 4734))
            addAll(12765..12768)
        }

    @JvmStatic
    fun bowLog(index: Int): FletchingLogDefinition? = bowLogs.getOrNull(index)

    @JvmStatic
    fun findBowLogByLog(logItemId: Int): FletchingLogDefinition? = bowLogs.firstOrNull { it.logItemId == logItemId }

    @JvmStatic
    fun findArrowRecipeByHead(headId: Int): ArrowRecipe? = arrowRecipes.firstOrNull { it.headId == headId }

    @JvmStatic
    fun findDartRecipeByTip(tipId: Int): DartRecipe? = dartRecipes.firstOrNull { it.tipId == tipId }

    @JvmStatic
    fun isBowWeapon(itemId: Int): Boolean = bowWeaponIds.contains(itemId)
}
