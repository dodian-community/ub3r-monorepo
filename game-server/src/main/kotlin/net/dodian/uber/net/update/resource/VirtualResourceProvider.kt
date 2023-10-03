package net.dodian.uber.net.update.resource

import org.apollo.cache.IndexedFileSystem

class VirtualResourceProvider(
    private val fs: IndexedFileSystem
) : ResourceProvider {

    private val validPrefixes = listOf(
        "/crc", "/title", "/config", "/interface", "/media", "/versionlist", "/textures", "/wordenc", "/sounds"
    )

    override fun accept(path: String) = validPrefixes.any(path::startsWith)

    override fun get(path: String) = when {
        path.startsWith("/crc") -> fs.crcTable
        path.startsWith("/title") -> fs.getFile(0, 1)
        path.startsWith("/config") -> fs.getFile(0, 2)
        path.startsWith("/interface") -> fs.getFile(0, 3)
        path.startsWith("/media") -> fs.getFile(0, 4)
        path.startsWith("/versionlist") -> fs.getFile(0, 5)
        path.startsWith("/textures") -> fs.getFile(0, 6)
        path.startsWith("/wordenc") -> fs.getFile(0, 7)
        path.startsWith("/sounds") -> fs.getFile(0, 8)
        else -> null
    }
}