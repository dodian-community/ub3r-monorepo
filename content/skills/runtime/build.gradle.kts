plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":skills:api"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.14.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.withType<Test> { useJUnitPlatform() }
