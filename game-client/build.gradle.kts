plugins {
    application
}

application {
    tasks.run.get().workingDir = rootProject.projectDir
    mainClass.set("net.dodian.uber.client.GameClientKt")
}

dependencies {
    implementation(project(":dodian-library"))

    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-compress:1.21")
}
tasks {

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes["Main-Class"] = "net.dodian.uber.client.GameClientKt"
            attributes["Class-Path"] = "net.dodian.uber.client"
        }

        from(sourceSets.main.get().output)
        dependsOn(configurations.runtimeClasspath)

        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
    }
}