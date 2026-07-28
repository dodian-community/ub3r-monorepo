plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":content-platform:api"))
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.withType<Test> { useJUnitPlatform() }
