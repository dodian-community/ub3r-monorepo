plugins {
    kotlin("jvm") version "1.6.21"
    id("application")
    `java-library`
}

application {
    mainClass.set("net.dodian.uber.game.Server")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val syncTestSourceSet = sourceSets.create("syncTest") {
    java.srcDirs("src/syncTest/java", "src/syncTest/kotlin")
    resources.srcDir("src/syncTest/resources")
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")

    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("org.apache.commons:commons-compress:1.21")

    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.mybatis:mybatis:3.5.10")

    implementation("com.google.code.gson:gson:2.7")

    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("com.google.guava:guava:33.1.0-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("com.h2database:h2:2.2.224")

    implementation("com.sparkjava:spark-kotlin:1.0.0-alpha")

    implementation(kotlin("reflect"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configurations[syncTestSourceSet.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[syncTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("syncTest") {
    description = "Runs focused synchronization and transport verification tests"
    group = "verification"
    testClassesDirs = syncTestSourceSet.output.classesDirs
    classpath = syncTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

tasks.register<JavaExec>("runSyncBenchmark") {
    group = "verification"
    description = "Run the synchronization pipeline benchmark harness"
    classpath = syncTestSourceSet.runtimeClasspath
    mainClass.set("net.dodian.uber.game.runtime.sync.SyncPipelineBenchmark")
}


