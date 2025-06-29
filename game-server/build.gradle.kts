plugins {
    kotlin("jvm") version "1.6.21"
    id("application")
    `java-library`
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

    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")

    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.quartz-scheduler:quartz:2.5.0")

    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.mybatis:mybatis:3.5.10")

    implementation("com.google.code.gson:gson:2.7")

    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("com.google.guava:guava:33.1.0-jre")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}