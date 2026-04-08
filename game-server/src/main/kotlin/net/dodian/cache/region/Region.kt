package net.dodian.cache.region

import net.dodian.utilities.Rangable

object Region {
    @JvmStatic
    fun load() {
        // Transitional compatibility: region clipping stays backed by Rangable map data.
    }

    @JvmStatic
    fun addClippingForVariableObject(x: Int, y: Int, height: Int, type: Int, direction: Int, flag: Boolean) {
        Rangable.addClippingForVariableObject(x, y, height, type, direction, flag)
    }

    @JvmStatic
    fun removeClippingForVariableObject(x: Int, y: Int, height: Int, type: Int, direction: Int, flag: Boolean) {
        Rangable.removeClippingForVariableObject(x, y, height, type, direction, flag)
    }

    @JvmStatic
    fun canMove(startX: Int, startY: Int, endX: Int, endY: Int, height: Int, xLength: Int, yLength: Int): Boolean {
        return Rangable.canMove(startX, startY, endX, endY, height, xLength, yLength)
    }
}
