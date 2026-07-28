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
project(":routefinder").projectDir = file("content/routefinder")

include(":skills")
project(":skills").projectDir = file("content/skills")

file("content/skills").listFiles()
    ?.filter { candidate -> candidate.isDirectory && candidate.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { candidate ->
        include(":skills:${candidate.name}")
        project(":skills:${candidate.name}").projectDir = candidate
    }

include(":quests")
project(":quests").projectDir = file("content/quests")

file("content/quests").listFiles()
    ?.filter { candidate -> candidate.isDirectory && candidate.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { candidate ->
        include(":quests:${candidate.name}")
        project(":quests:${candidate.name}").projectDir = candidate
    }
