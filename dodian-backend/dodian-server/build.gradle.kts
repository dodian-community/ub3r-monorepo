dependencies {
    implementation(project(":dodian-common:dodian-library"))
    implementation(project(":dodian-backend:dodian-scripting"))

    implementation("com.michael-bull.kotlin-result:kotlin-result-jvm:1.1.16")

    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-cors:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-jackson:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.4")
    implementation("io.ktor:ktor-server-auth:2.3.4")

    implementation("io.github.cdimascio:dotenv-kotlin:6.3.1")
    implementation("org.apache.commons:commons-compress:1.21")

    // TODO: Remove these dependencies
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("org.mybatis:mybatis:3.5.10")
}