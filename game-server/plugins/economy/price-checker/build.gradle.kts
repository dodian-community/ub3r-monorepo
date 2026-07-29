plugins { id("ub3r.content-plugin") }

contentModule {
    moduleId.set("economy.price-checker")
    family.set("economy")
    implementationClass.set("net.dodian.uber.economy.pricechecker.PriceCheckerModule")
}

dependencies { implementation(project(":economy:api")) }
