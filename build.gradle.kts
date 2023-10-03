plugins {
    kotlin("jvm") version "1.9.10" apply false
}

allprojects {

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")
}