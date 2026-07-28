package net.dodian.uber.game.engine.config

import java.nio.file.Files
import java.nio.file.Path

/** Resolves server-owned files consistently from either the repository root or game-server. */
object ServerPaths {
    @JvmStatic
    fun gameServerRoot(): Path {
        val workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        if (workingDirectory.fileName?.toString() == "game-server") {
            return workingDirectory
        }
        val nested = workingDirectory.resolve("game-server")
        return if (Files.isDirectory(nested)) nested else workingDirectory
    }

    @JvmStatic
    fun definition(vararg segments: String): Path = resolve("definitions", segments)

    @JvmStatic
    fun revision218Reference(vararg segments: String): Path =
        resolve("reference/cache-rev218", segments)

    private fun resolve(directory: String, segments: Array<out String>): Path {
        var path = gameServerRoot().resolve(directory)
        for (segment in segments) {
            require(segment.isNotBlank() && segment != "." && segment != "..") {
                "Invalid server path segment '$segment'"
            }
            path = path.resolve(segment)
        }
        return path.normalize()
    }
}
