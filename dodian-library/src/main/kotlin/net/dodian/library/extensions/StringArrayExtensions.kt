package net.dodian.library.extensions

fun Array<String>.argument(key: String) = this.firstOrNull {
    it.lowercase().startsWith("--${key.lowercase()}")
}?.split("=")?.get(1)