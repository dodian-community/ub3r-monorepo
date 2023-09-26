plugins {
    kotlin("jvm") version "1.9.10"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }
}