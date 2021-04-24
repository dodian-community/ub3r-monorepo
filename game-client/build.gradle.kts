plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "Client"
}

tasks.register<JavaExec>("runClient") {
    group = "dodian-game"
    dependsOn("run")
}