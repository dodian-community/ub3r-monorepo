package net.dodian.extensions

import kotlin.io.path.Path

fun String.toPath() = Path(this)