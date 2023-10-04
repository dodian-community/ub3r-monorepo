dependencies {
    implementation(project(":dodian-common:dodian-library"))

    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.apache.commons:commons-collections4:4.4")
}

fun JavaExec.configure(
    host: String = "127.0.0.1",
    gamePort: Int = 43594,
    filePort: Int = 43595,
    httpPort: Int = 8080
) {
    group = "dodian-client"

    workingDir = project.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.uber.client.GameClientKt")
    args = listOf(
        "--host=$host",
        "--gamePort=$gamePort",
        "--filePort=$filePort",
        "--httpPort=$httpPort"
    )
}

tasks.register<JavaExec>("localhostRun") {
    configure()
}

tasks.register<JavaExec>("liveRun") {
    configure(host = "play.dodian.net", httpPort = 43596)
}