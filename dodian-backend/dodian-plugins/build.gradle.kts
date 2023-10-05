allprojects {

    dependencies {
        compileOnly(project(":dodian-backend:dodian-server"))
        compileOnly(project(":dodian-backend:dodian-scripting"))
    }
}

subprojects {

    dependencies {
        compileOnly(project(":dodian-backend:dodian-content"))

        implementation("org.pf4j:pf4j:3.10.0")
    }

    tasks.named<Jar>("jar") {
        manifest {
            attributes["Plugin-Id"] = project.name
            attributes["Plugin-Version"] = "0.1.0"
            attributes["Plugin-Provider"] = "Nozemi"
        }
    }
}