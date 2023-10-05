import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

dependencies {
    implementation(project(":dodian-common:dodian-library"))
    implementation(project(":dodian-common:dodian-cache"))
    implementation(project(":dodian-backend:dodian-scripting"))

    implementation("com.michael-bull.kotlin-result:kotlin-result-jvm:1.1.16")

    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-cors:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-jackson:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")

    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")
    implementation("org.apache.commons:commons-compress:1.21")

    // TODO: Remove these dependencies
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("org.mybatis:mybatis:3.5.10")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xallow-any-scripts-in-source-roots")
        jvmTarget = "17"
    }
}

tasks.register<JavaExec>("launchServer") {
    group = "dodian-server"

    workingDir = project.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.GameServerKt")
}

tasks.register<JavaExec>("generateRsa") {
    group = "dodian-server"

    workingDir = project.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.uber.utils.RsaUtilsKt")
    args = listOf("--bitCount=1024", "--path=./data/rsa")
}

tasks.register<JavaExec>("installCleanCache") {
    group = "dodian-server"

    workingDir = project.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.cache.CacheDownloaderKt")
    args = listOf("--path=${project.projectDir}/data", "--revision=317")
}