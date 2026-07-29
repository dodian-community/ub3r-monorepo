plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":social:api"))
    api(project(":social:runtime"))
}
