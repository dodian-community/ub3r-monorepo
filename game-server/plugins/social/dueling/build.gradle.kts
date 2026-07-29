plugins { id("ub3r.content-plugin") }

contentModule {
    moduleId.set("social.dueling")
    family.set("social")
    implementationClass.set("net.dodian.uber.social.dueling.DuelingModule")
}

dependencies {
    implementation(project(":social:api"))
}
