plugins {
    kotlin("jvm") version "1.9.10"
}

allprojects {

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

        implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.5")
    }
}