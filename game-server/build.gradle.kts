plugins {
    application
}

application {
    mainClass.set("net.dodian.GameServerKt")
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

    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.quartz-scheduler:quartz:2.3.2")

    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("org.mybatis:mybatis:3.5.10")

    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("io.netty:netty-all:4.1.94.Final")
    implementation("com.google.guava:guava:19.0")

    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")

    implementation(project(":game-utilities:game-scripting"))
}

allprojects {

    dependencies {
        implementation("io.github.classgraph:classgraph:4.8.162")

        implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.10")
        implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.10")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")

        implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.5")
        implementation("com.michael-bull.kotlin-result:kotlin-result-jvm:1.1.16")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xallow-any-scripts-in-source-roots")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<JavaExec>("generateRsaKeypair") {
    group = "dodian-utils"

    workingDir = project.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.uber.utils.RsaUtilsKt")
    args = listOf("--bitCount=1024", "--path=./data/rsa")
}