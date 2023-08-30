plugins {
    kotlin("jvm") version "1.9.10"
}

application {
    mainClass.set("net.dodian.ServerBootstrapKt")
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

repositories {
    maven { url = uri("https://repo.openrs2.org/repository/openrs2") }
    maven { url = uri("https://repo.openrs2.org/repository/openrs2-snapshots") }
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.quartz-scheduler:quartz:2.3.2")

    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("org.mybatis:mybatis:3.5.10")

    implementation("com.google.guava:guava:19.0")

    implementation("ch.qos.logback:logback-classic:1.4.7")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("io.netty:netty-all:4.1.94.Final")

    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.5")
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

tasks {

    register<JavaExec>("generateRsaKeypair") {
        group = "dodian-utils"

        workingDir = project.projectDir
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("net.dodian.cli.RsaGeneratorKt")
        args = listOf("16", "2048", "./data/rsa")
    }
}