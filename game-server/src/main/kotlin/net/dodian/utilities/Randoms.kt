package net.dodian.utilities

object Randoms {
    @JvmStatic
    fun random(range: Int): Int {
        val number = (Math.random() * (range + 1)).toInt()
        return if (number < 0) 0 else number
    }

    @JvmStatic
    fun randomMinusOne(range: Int): Int {
        val number = (Math.random() * range).toInt()
        return if (number < 0) 0 else number
    }

    @JvmStatic
    fun chance(range: Int): Int {
        val safeRange = if (range < 1) 0 else range
        return (Math.random() * safeRange + 1).toInt()
    }

    @JvmStatic
    fun random2(range: Int): Int = (Math.random() * range + 1).toInt()

    @JvmStatic
    fun dRandom2(range: Double): Double = Math.random() * range + 1
}