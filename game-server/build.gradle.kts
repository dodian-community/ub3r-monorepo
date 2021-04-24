import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springVersion: String by project

plugins {
    application
    kotlin("jvm")
    kotlin("plugin.spring") version "1.3.72"
    kotlin("plugin.jpa") version "1.3.72"
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
}

application {
    mainClassName = "net.dodian.uber.game.Server"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.apache.commons:commons-compress:1.20")
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("mysql:mysql-connector-java:8.0.24")
}