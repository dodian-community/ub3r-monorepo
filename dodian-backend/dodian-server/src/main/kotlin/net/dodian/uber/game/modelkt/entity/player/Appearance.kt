package net.dodian.uber.game.modelkt.entity.player

data class Appearance(
    val gender: Gender,
    val style: List<Int>,
    val colors: List<Int>
) {
    enum class Gender {
        MALE,
        FEMALE
    }

    val isFemale: Boolean get() = gender == Gender.FEMALE
    val isMale: Boolean get() = gender == Gender.MALE

    companion object {
        val DEFAULT = Appearance(
            gender = Gender.MALE,
            style = listOf(0, 10, 18, 26, 33, 36, 42),
            colors = listOf(0, 0, 0, 0, 0)
        )
    }
}