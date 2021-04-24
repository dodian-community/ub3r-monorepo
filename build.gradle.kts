plugins {
    application
    kotlin("jvm") version "1.3.70"
}

dependencies {
    compileOnly(project(":game-client"))
    compileOnly(project(":game-server"))
    compileOnly(project(":game-launcher"))
}

allprojects {

    repositories {
        mavenCentral()
        jcenter()
    }
}