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

    implementation("com.sparkjava:spark-kotlin:1.0.0-alpha")
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

// Custom task to run our validation test
tasks.register<JavaExec>("runValidationTest") {
    group = "verification"
    description = "Run PlayerUpdateMessageHelper validation test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerUpdateMessageHelperTest")
}

// Custom task to run movement migration test
tasks.register<JavaExec>("runMovementMigrationTest") {
    group = "verification"
    description = "Run PlayerUpdating movement migration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerMovementMigrationTest")
}

// Custom task to run main update method migration test
tasks.register<JavaExec>("runUpdateNettyMigrationTest") {
    group = "verification"
    description = "Run PlayerUpdating main update method migration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerUpdateNettyMigrationTest")
}

// Custom task to run appendBlockUpdate migration test
tasks.register<JavaExec>("runUpdateBlockMigrationTest") {
    group = "verification"
    description = "Run PlayerUpdating appendBlockUpdate migration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerUpdateBlockMigrationTest")
}

// Custom task to run player appearance migration test
tasks.register<JavaExec>("runAppearanceMigrationTest") {
    group = "verification"
    description = "Run PlayerUpdating appearance migration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerAppearanceMigrationTest")
}

// Custom task to run main update method migration test
tasks.register<JavaExec>("runMainUpdateMigrationTest") {
    group = "verification"
    description = "Run PlayerUpdating main update method migration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerMainUpdateMigrationTest")
}

// Custom task to run coordinate and combat migration test
tasks.register<JavaExec>("runCoordinateCombatMigrationTest") {
    group = "verification"
    description = "Run PlayerUpdating coordinate and combat migration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerCoordinateCombatMigrationTest")
}

// Custom task to run method signature migration test
tasks.register<JavaExec>("runMethodSignatureMigrationTest") {
    group = "verification"
    description = "Run PlayerUpdating method signature migration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerMethodSignatureMigrationTest")
}

// Custom task to run PacketBuffer cleanup test
tasks.register<JavaExec>("runPacketBufferCleanupTest") {
    group = "verification"
    description = "Run PlayerUpdating PacketBuffer cleanup test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerPacketBufferCleanupTest")
}

// Custom task to run comprehensive validation test
tasks.register<JavaExec>("runComprehensiveTest") {
    group = "verification"
    description = "Run comprehensive PlayerUpdating validation test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerUpdatingComprehensiveTest")
}

// Custom task to run performance benchmark
tasks.register<JavaExec>("runPerformanceBenchmark") {
    group = "verification"
    description = "Run PlayerUpdating performance benchmark"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerUpdatingPerformanceBenchmark")
}

// Custom task to run integration test
tasks.register<JavaExec>("runIntegrationTest") {
    group = "verification"
    description = "Run PlayerUpdating integration test"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    mainClass.set("net.dodian.uber.game.model.entity.player.PlayerUpdatingIntegrationTest")
}

tasks.register<JavaExec>("exportWorldFromCache") {
    group = "build"
    description = "Export data/world map+object files from data/cache for server clipping/object loaders"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.dodian.cache.tools.CacheWorldExporter")
}

// Custom task to run all migration tests
tasks.register<JavaExec>("runAllMigrationTests") {
    group = "verification"
    description = "Run all PlayerUpdating migration tests"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    
    doFirst {
        println("Running all PlayerUpdating migration tests...")
    }
}
