pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}

rootProject.name = "ub3r"

include(":game-server")
include(":game-client")
include(":routefinder")
include(":stress-client")

file("skills").listFiles()
    ?.filter { candidate -> candidate.isDirectory && candidate.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { candidate -> include(":skills:${candidate.name}") }

file("quests").listFiles()
    ?.filter { candidate -> candidate.isDirectory && candidate.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { candidate -> include(":quests:${candidate.name}") }
