plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

application {
    mainClass.set("net.dodian.stress.StressClientLauncher")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "net.dodian.stress.StressClientLauncher"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
