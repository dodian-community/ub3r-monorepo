import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("jvm") version "1.9.10" apply false
}

subprojects {
    if (!path.startsWith(":skills:") && !path.startsWith(":quests:")) {
        apply(plugin = "application")
    }
    apply(plugin = "java")

    repositories {
        mavenCentral()
        maven(url = "https://repo.runelite.net")
        maven(url = "https://jitpack.io")
    }
}

val skillModuleProjects = subprojects.filter {
    it.parent?.path == ":skills" && it.name !in setOf("api", "runtime", "testkit")
}

skillModuleProjects.forEach { skillProject ->
    skillProject.dependencies {
        add("testImplementation", "org.junit.jupiter:junit-jupiter-api:5.9.3")
        add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.9.3")
    }
    skillProject.tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        doFirst {
            val hasKotlinTests = skillProject.file("src/test/kotlin").walkTopDown()
                .any { it.isFile && it.extension == "kt" }
            check(hasKotlinTests) {
                "${skillProject.path} has no live runtime test source; descriptor-only skill modules are not allowed."
            }
        }
    }
}

tasks.register("skillsCheck") {
    group = "verification"
    description = "Verifies the shared skill platform and every independently compiled skill module."
    dependsOn(":skills:api:check", ":skills:runtime:check", ":skills:testkit:check")
    dependsOn(skillModuleProjects.map { "${it.path}:check" })
}

tasks.register("verifySkillModuleDependencies") {
    group = "verification"
    description = "Rejects server dependencies from independently compiled skill modules and shared test fixtures."
    doLast {
        val offenders = file("content/skills").walkTopDown()
            .filter { it.name == "build.gradle.kts" }
            .filter { it.readText().contains("project(\":game-server\")") }
            .toList()
        check(offenders.isEmpty()) {
            "Skill modules must not depend on :game-server: ${offenders.joinToString { it.relativeTo(projectDir).path }}"
        }
    }
}

tasks.named("skillsCheck") { dependsOn("verifySkillModuleDependencies") }

val questModuleProjects = subprojects.filter {
    it.parent?.path == ":quests" && it.name !in setOf("api", "runtime", "testkit")
}

questModuleProjects.forEach { questProject ->
    questProject.dependencies {
        add("testImplementation", "org.junit.jupiter:junit-jupiter-api:5.9.3")
        add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.9.3")
    }
    questProject.tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        doFirst {
            val hasKotlinTests = questProject.file("src/test/kotlin").walkTopDown()
                .any { it.isFile && it.extension == "kt" }
            check(hasKotlinTests) {
                "${questProject.path} has no live runtime test source; descriptor-only quest modules are not allowed."
            }
        }
    }
}

tasks.register("questsCheck") {
    group = "verification"
    description = "Verifies the shared quest platform and every independently compiled quest module."
    dependsOn(":quests:api:check", ":quests:runtime:check", ":quests:testkit:check")
    dependsOn(questModuleProjects.map { "${it.path}:check" })
}

tasks.register("verifyQuestModuleDependencies") {
    group = "verification"
    description = "Rejects server dependencies from independently compiled quest modules."
    doLast {
        val offenders = file("content/quests").walkTopDown()
            .filter { it.name == "build.gradle.kts" }
            .filter { it.readText().contains("project(\":game-server\")") }
            .toList()
        check(offenders.isEmpty()) {
            "Quest modules must not depend on :game-server: ${offenders.joinToString { it.relativeTo(projectDir).path }}"
        }
    }
}

tasks.named("questsCheck") { dependsOn("verifyQuestModuleDependencies") }
