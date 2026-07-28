plugins {
    kotlin("jvm")
    `java-library`
    id("me.champeau.jmh") version "0.7.2"
}

group = "org.rsmod"
version = "6.0.0"
description = "Vendored RS Mod BFS routefinder"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    explicitApi()
    jvmToolchain(11)
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    jmh("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    jmh("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

tasks.test {
    useJUnitPlatform()
}
