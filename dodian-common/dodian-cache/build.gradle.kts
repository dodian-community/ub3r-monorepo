dependencies {
    implementation(project(":dodian-common:dodian-library"))
}

tasks.register<JavaExec>("downloadCleanCache") {
    group = "dodian-cache"

    workingDir = project.projectDir
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.dodian.cache.CacheDownloaderKt")
    args = listOf("--path=${project.projectDir}/data", "--revision=317")
}