package net.dodian.server.cache.types

interface Type
interface TypeBuilder<T : Type> {
    fun build(): T
}