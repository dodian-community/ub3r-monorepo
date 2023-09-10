plugins {
    application
}

application {
    mainClass.set("net.dodian.client.GameClient")
}

java {
    charset("UTF-8")

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.apache.commons:commons-collections4:4.4")
}