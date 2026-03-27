package net.dodian.utilities

class Range(
    private val floor: Int,
    private val ceiling: Int,
) {
    fun getFloor(): Int = floor

    fun getCeiling(): Int = ceiling

    val value: Int
        get() = getValue()

    fun getValue(): Int = floor + (Math.random() * (ceiling - floor + 1)).toInt()
}
