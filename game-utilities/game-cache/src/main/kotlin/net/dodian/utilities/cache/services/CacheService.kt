package net.dodian.utilities.cache.services

import com.displee.cache.CacheLibrary

class CacheService(
    private val path: String = "./data/cache",
    val cache: CacheLibrary = CacheLibrary(path)
)