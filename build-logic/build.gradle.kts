plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
}

gradlePlugin {
    plugins {
        create("ub3rSkillPlugin") {
            id = "ub3r.skill-plugin"
            implementationClass = "net.dodian.uber.buildlogic.Ub3rSkillPlugin"
        }
        create("ub3rQuestPlugin") {
            id = "ub3r.quest-plugin"
            implementationClass = "net.dodian.uber.buildlogic.Ub3rQuestPlugin"
        }
        create("ub3rContentPlugin") {
            id = "ub3r.content-plugin"
            implementationClass = "net.dodian.uber.buildlogic.Ub3rContentPlugin"
        }
    }
}
