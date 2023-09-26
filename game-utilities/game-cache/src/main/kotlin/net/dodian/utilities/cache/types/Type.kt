package net.dodian.utilities.cache.types

interface Type
interface TypeBuilder<T : Type> {
    fun build(): T
}