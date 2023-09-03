plugins {
    application
}

application {
    mainClass.set("net.dodian.client.GameClient")
}

dependencies {
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.apache.commons:commons-collections4:4.4")
}