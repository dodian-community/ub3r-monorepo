dependencies {
    implementation(project(":dodian-common:dodian-library"))

    implementation("com.displee:disio:2.2")
    implementation("com.displee:rs-cache-library:6.9")

    implementation("io.netty:netty-all:4.1.93.Final")
    implementation("org.apache.commons:commons-math3:3.6.1")

    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-cors:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-jackson:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")
}

tasks.register<JavaExec>("dumpCache") {
    group = "dodian-cache"

    workingDir = project.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.utilities.cache.services.CacheDumperServiceKt")
    args = listOf("--cacheDir=${project(":dodian-backend:dodian-server").projectDir.resolve("data").resolve("cache")}")
}