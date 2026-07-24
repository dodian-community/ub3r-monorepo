package net.dodian.uber.skills.skillguide

import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillSupportDefinition
import net.dodian.uber.game.api.plugin.skills.SkillSupportModule
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillSupportModule
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.skills.runtime.TomlRecordReader

data class SkillGuideDef(
    val skillName: String,
    val skillId: Int,
    val sections: List<String>,
)

object SkillguideModule : SkillSupportModule {
    val descriptor = SkillModuleDescriptor("skill.skillguide", "Skillguide")
    val guides: List<SkillGuideDef> by lazy { loadGuides() }

    override val definition: SkillSupportDefinition = skillSupportModule("Skillguide") {
        // Support module for skill interface guides
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "support",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    private fun loadGuides(): List<SkillGuideDef> =
        TomlRecordReader.readRecords("skillguide/guides.toml", "guide").map { row ->
            val sectionsStr = row.getValue("sections").removeSurrounding("[", "]").split(",")
            SkillGuideDef(
                skillName = row.getValue("skill_name"),
                skillId = row.getValue("skill_id").toInt(),
                sections = sectionsStr.map { it.trim().trim('"') },
            )
        }
}
