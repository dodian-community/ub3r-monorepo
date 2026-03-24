package net.dodian.uber.game.skills.guide

data class SkillGuideLayout(
    val showComponents: IntArray = intArrayOf(),
    val hideComponents: IntArray = intArrayOf(),
    val extraStrings: Map<Int, String> = emptyMap(),
)
