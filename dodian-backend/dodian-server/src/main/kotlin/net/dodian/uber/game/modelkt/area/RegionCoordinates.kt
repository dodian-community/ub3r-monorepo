package net.dodian.uber.game.modelkt.area

data class RegionCoordinates(
    val x: Int,
    val y: Int
) {
    val absoluteX: Int get() = Region.SIZE * (x + 6)
    val absoluteY: Int get() = Region.SIZE * (y + 6)
    override fun hashCode() = x shl 16 or y
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is RegionCoordinates)
            return false

        return x == other.x && y == other.y
    }

    companion object {
        fun fromPosition(position: Position) = RegionCoordinates(position.topLeftRegionX, position.topLeftRegionY)
    }
}