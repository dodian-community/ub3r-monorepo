java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("com.runescape.Client")
}

sourceSets {
    getByName("main") {
        java.setSrcDirs(listOf("src"))
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.runescape.Client"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
