package net.dodian.server.cache.extensions


const val ARCHIVE_TITLE = 1
const val ARCHIVE_CONFIG = 2
const val ARCHIVE_INTERFACES = 3
const val ARCHIVE_GRAPHICS = 4
const val ARCHIVE_UPDATE = 5
const val ARCHIVE_TEXTURES = 6
const val ARCHIVE_CHAT = 7
const val ARCHIVE_SOUND = 8

enum class ConfigType(val group: String, val index: String) {
    Npc("npc.dat", "npc.idx")
}
