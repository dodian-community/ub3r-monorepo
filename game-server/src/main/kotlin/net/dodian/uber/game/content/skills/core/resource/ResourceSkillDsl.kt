package net.dodian.uber.game.content.skills.core.resource

typealias HarvestNodeDef = net.dodian.uber.game.systems.skills.resource.HarvestNodeDef
typealias ToolDef = net.dodian.uber.game.systems.skills.resource.ToolDef
typealias ResourceSkillContent = net.dodian.uber.game.systems.skills.resource.ResourceSkillContent
typealias ResourceSkillContentBuilder = net.dodian.uber.game.systems.skills.resource.ResourceSkillContentBuilder
typealias ResourceFamilyBuilder = net.dodian.uber.game.systems.skills.resource.ResourceFamilyBuilder
typealias HarvestNodeBuilder = net.dodian.uber.game.systems.skills.resource.HarvestNodeBuilder
typealias ToolBuilder = net.dodian.uber.game.systems.skills.resource.ToolBuilder

fun resourceSkillContent(block: ResourceSkillContentBuilder.() -> Unit): ResourceSkillContent =
    net.dodian.uber.game.systems.skills.resource.resourceSkillContent(block)
