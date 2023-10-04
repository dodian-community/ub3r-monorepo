subprojects {

    dependencies {
        implementation("io.github.classgraph:classgraph:4.8.162")

        implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.10")
        implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.10")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")

        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
        implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

        implementation("org.apache.logging.log4j:log4j-api:2.20.0")
        implementation("org.apache.logging.log4j:log4j-core:2.20.0")
        implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")

        implementation("com.displee:disio:2.2")
        implementation("com.displee:rs-cache-library:6.9")

        implementation("io.netty:netty-all:4.1.93.Final")
        implementation("org.bouncycastle:bcprov-jdk15on:1.70")
        implementation("com.google.guava:guava:19.0")
    }
}