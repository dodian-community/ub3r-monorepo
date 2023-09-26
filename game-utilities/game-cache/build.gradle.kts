plugins {
    application
}

application {
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
}