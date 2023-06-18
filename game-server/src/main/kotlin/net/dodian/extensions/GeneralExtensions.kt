package net.dodian.extensions

import java.io.File
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.full.starProjectedType

val KClass<*>.isMarkedNullable get() = this.starProjectedType.isMarkedNullable

fun String.toPath() = Paths.get(this)
fun String.toFile() = File(this)

fun <T> Collection<T>.containsAny(vararg values: T): Boolean {
    values.forEach {
        if (this.any { source -> source == it }) return true
    }

    return false
}