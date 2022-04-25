java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("com.fox.Launcher")
}

tasks.jar {

    manifest {
        attributes["Main-Class"] = "net.dodian.client.Client"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}