package net.dodian.uber.game.modelkt.area

data class RegionRepository(
    val permitRemoval: Boolean,
    private val regions: MutableMap<RegionCoordinates, Region> = mutableMapOf(),
    private val defaultRegionListeners: MutableList<RegionListener> = mutableListOf()
) {

    private fun add(region: Region) {
        if (regions.containsKey(region.coordinates) && !permitRemoval)
            error("Cannot add a new Region with the same coordinates as an existing Region.")

        defaultRegionListeners.forEach(region::addListener)
        regions[region.coordinates] = region
    }

    operator fun get(coordinates: RegionCoordinates): Region {
        var region = regions[coordinates]
        if (region == null) {
            region = Region(coordinates)
            add(region)
        }

        return region
    }

    fun fromPosition(position: Position) = get(RegionCoordinates.fromPosition(position))

    companion object {
        val immutable = RegionRepository(false)
        val mutable = RegionRepository(true)
    }
}