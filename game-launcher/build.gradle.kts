import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.9.10"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("net.dodian.launcher.seamless.ApplicationKt")
}

tasks {

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes["Main-Class"] = "net.dodian.launcher.seamless.ApplicationKt"
            attributes["Class-Path"] = "net.dodian.launcher.seamless"
        }

        from(sourceSets.main.get().output)
        dependsOn(configurations.runtimeClasspath)

        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}