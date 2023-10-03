package net.dodian.uber.net.update.resource

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

class HyperTextResourceProvider(
    private val base: Path
) : ResourceProvider {

    override fun accept(path: String): Boolean {
        var file = base.resolve(path)

        val target = file.toUri().normalize()
        if (!target.toASCIIString().startsWith(base.toUri().normalize().toASCIIString()))
            return false

        if (Files.isDirectory(file))
            file = file.resolve("index.html")

        return Files.exists(file)
    }

    override fun get(path: String): ByteBuffer? {
        var root = base.resolve(path)

        if (Files.isDirectory(root))
            root = root.resolve("index.html")

        if (!Files.exists(root))
            return null

        val fileChannel = FileChannel.open(root)

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, Files.size(root))
    }
}