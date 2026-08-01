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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "ub3r"

include(":game-server")
include(":game-client")
include(":routefinder")
include(":stress-client")
project(":routefinder").projectDir = file("game-server/services/routefinder")

include(":content-platform")
project(":content-platform").projectDir = file("game-server/plugins/platform")
listOf("api", "runtime", "testkit").forEach { module ->
    include(":content-platform:$module")
    project(":content-platform:$module").projectDir = file("game-server/plugins/platform/$module")
}

include(":skills")
project(":skills").projectDir = file("game-server/plugins/skills")

file("game-server/plugins/skills").listFiles()
    ?.filter { candidate -> candidate.isDirectory && candidate.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { candidate ->
        include(":skills:${candidate.name}")
        project(":skills:${candidate.name}").projectDir = candidate
    }

include(":quests")
project(":quests").projectDir = file("game-server/plugins/quests")

file("game-server/plugins/quests").listFiles()
    ?.filter { candidate -> candidate.isDirectory && candidate.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { candidate ->
        include(":quests:${candidate.name}")
        project(":quests:${candidate.name}").projectDir = candidate
    }

include(":social")
project(":social").projectDir = file("game-server/plugins/social")
listOf("api", "runtime", "testkit", "trading", "dueling").forEach { module ->
    include(":social:$module")
    project(":social:$module").projectDir = file("game-server/plugins/social/$module")
}

include(":economy")
project(":economy").projectDir = file("game-server/plugins/economy")
listOf("api", "runtime", "shops", "price-checker").forEach { module ->
    include(":economy:$module")
    project(":economy:$module").projectDir = file("game-server/plugins/economy/$module")
}
