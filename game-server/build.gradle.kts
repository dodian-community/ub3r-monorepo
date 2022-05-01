plugins {
    kotlin("jvm") version "1.6.10"
}

application {
    mainClass.set("net.dodian.uber.game.Server")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.jar {

    manifest {
        attributes["Main-Class"] = "net.dodian.uber.game.Server"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output

    from(contents)
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("io.github.cdimascio:dotenv-kotlin:6.2.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.2.2")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.quartz-scheduler:quartz:2.3.2")

    implementation("mysql:mysql-connector-java:8.0.28")
}