package net.dodian.uber.game.engine.sync.template

data class PlayerSyncTemplateKey(
    val localSlots: IntArray,
    val localCount: Int,
    val selfMovementMode: Int,
    val selfUpdateRequired: Boolean,
    val teleporting: Boolean,
    val mapRegionChange: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerSyncTemplateKey) return false
        if (localCount != other.localCount) return false
        if (selfMovementMode != other.selfMovementMode) return false
        if (selfUpdateRequired != other.selfUpdateRequired) return false
        if (teleporting != other.teleporting) return false
        if (mapRegionChange != other.mapRegionChange) return false
        return localSlots.contentEquals(other.localSlots)
    }

    override fun hashCode(): Int {
        var result = localSlots.contentHashCode()
        result = 31 * result + localCount
        result = 31 * result + selfMovementMode
        result = 31 * result + selfUpdateRequired.hashCode()
        result = 31 * result + teleporting.hashCode()
        result = 31 * result + mapRegionChange.hashCode()
        return result
    }
}
