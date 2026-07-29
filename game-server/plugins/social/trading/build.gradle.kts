plugins { id("ub3r.content-plugin") }

contentModule {
    moduleId.set("social.trading")
    family.set("social")
    implementationClass.set("net.dodian.uber.social.trading.TradingModule")
}

dependencies {
    implementation(project(":social:api"))
}
