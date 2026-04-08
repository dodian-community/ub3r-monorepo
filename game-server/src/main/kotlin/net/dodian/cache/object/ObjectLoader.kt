package net.dodian.cache.`object`

class ObjectLoader {
    fun load() {
        // Transitional no-op: legacy cache decoding removed.
    }

    companion object {
        @JvmStatic
        fun `object`(x: Int, y: Int, z: Int): CacheObject? = null

        @JvmStatic
        fun `object`(id: Int, x: Int, y: Int, z: Int): CacheObject? = null

        @JvmStatic
        fun `object`(name: String, x: Int, y: Int, z: Int): CacheObject? = null
    }
}
