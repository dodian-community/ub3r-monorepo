package net.dodian.launcher.seamless

import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.pathString

val homeFolder: Path = Path(System.getProperty("user.home")).resolve(clientHomeFolder)

fun clientName(md5Hash: String) = "client-${md5Hash}.jar"

fun main() {
    initialize()
}

fun initialize() {
    if (!homeFolder.exists()) {
        homeFolder.toFile().mkdirs()
    }

    launchClient()
}

fun launchClient() {
    val versionContent = URL(clientVersionUrl).readText()
    val match = "Client File Hash: (\\w+)".toRegex().find(versionContent)
    val md5Hash = match?.groups?.get(1)?.value
        ?: error("Failed because no version could be found...")

    val clientJar = homeFolder.resolve(clientName(md5Hash))

    if (clientJar.toFile().exists()) {
        val builder = ProcessBuilder("javaw", "-jar", clientJar.pathString)
        val process = builder.start()

        println("Launching existing up to date client...")
    } else {
        println("Client '${clientName(md5Hash)}' doesn't exist at '${homeFolder}', need to download...")
        downloadClient(md5Hash)

        launchClient()
    }
}

fun downloadClient(md5Hash: String) {
    URL(clientDownloadUrl).openStream().use {
        Channels.newChannel(it).use { readableChannel ->
            FileOutputStream(homeFolder.resolve(clientName(md5Hash)).pathString).use { fileOutputStream ->
                fileOutputStream.channel.transferFrom(readableChannel, 0, Long.MAX_VALUE)
            }
        }
    }
}