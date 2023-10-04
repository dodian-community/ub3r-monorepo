package net.dodian.utilities.cache.types

import com.displee.cache.CacheLibrary

interface Type
interface TypeBuilder<T : Type> {
    fun build(): T
}

interface TypeLoader<T : Type> {
    fun load(cache: CacheLibrary): List<T>
}