plugins {
    application
}

application {
    tasks.run.get().workingDir = rootProject.projectDir
    mainClass.set("net.dodian.utilities.cache.CacheServiceAppKt")
}

dependencies {
    implementation("com.displee:disio:2.2")
    implementation("com.displee:rs-cache-library:6.9")

    implementation("io.netty:netty-all:4.1.93.Final")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.5")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")

    implementation("org.apache.commons:commons-math3:3.6.1")

    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-cors:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-jackson:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")

    implementation("com.twelvemonkeys.imageio:imageio-webp:3.8.2")
}

tasks.register<JavaExec>("dump-all") {
    group = "dodian-cache"

    workingDir = rootProject.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.utilities.cache.services.CacheDumperServiceKt")
}