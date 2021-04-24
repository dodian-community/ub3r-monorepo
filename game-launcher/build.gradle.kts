plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = ""
}

tasks.register<JavaExec>("runLauncher") {
    group = "dodian-game"
    dependsOn("run")
}