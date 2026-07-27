plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":quests:api"))
    testImplementation(project(":quests:testkit"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.withType<Test> { useJUnitPlatform() }
