plugins { id("ub3r.skill-plugin") }
skillModule {
    implementationClass.set("net.dodian.uber.skills.skillguide.SkillguideModule")
    kind.set(net.dodian.uber.buildlogic.SkillModuleKind.SUPPORT)
}
