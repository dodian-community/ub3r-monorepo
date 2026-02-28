java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
