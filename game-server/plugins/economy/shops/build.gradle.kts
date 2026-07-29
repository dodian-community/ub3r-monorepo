plugins { id("ub3r.content-plugin") }

contentModule {
    moduleId.set("economy.shops")
    family.set("economy")
    implementationClass.set("net.dodian.uber.economy.shops.ShopsModule")
}

dependencies { implementation(project(":economy:api")) }
