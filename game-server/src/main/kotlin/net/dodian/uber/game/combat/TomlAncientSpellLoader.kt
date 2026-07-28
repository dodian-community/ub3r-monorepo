package net.dodian.uber.game.combat

import java.nio.file.Files
import java.nio.file.Path
import net.dodian.uber.game.engine.config.ServerPaths

data class AncientSpellDef(
    val index: Int,
    val name: String,
    val requiredLevel: Int,
    val baseDamage: Int,
    val spellId: Int,
    val buttonId: Int,
    val coolDown: Int,
    val castGfx: String? = null,
    val projectileGfx: String? = null,
    val impactGfx: String? = null,
    val castAnim: Int = 1979,
)

object TomlAncientSpellLoader {
    @JvmStatic
    fun load(path: String = ServerPaths.definition("combat", "ancient_spells.toml").toString()): List<AncientSpellDef> {
        val file = Path.of(path)
        if (!Files.isRegularFile(file)) {
            return emptyList()
        }

        val entries = mutableListOf<AncientSpellDef>()
        var index = -1
        var name = ""
        var requiredLevel = -1
        var baseDamage = -1
        var spellId = -1
        var buttonId = -1
        var coolDown = -1
        var castGfx: String? = null
        var projectileGfx: String? = null
        var impactGfx: String? = null
        var castAnim = 1979
        var inEntry = false
        var hasIndex = false
        var hasSpellId = false

        fun flush() {
            if (inEntry && hasIndex && hasSpellId) {
                entries += AncientSpellDef(
                    index = index,
                    name = name,
                    requiredLevel = requiredLevel,
                    baseDamage = baseDamage,
                    spellId = spellId,
                    buttonId = buttonId,
                    coolDown = coolDown,
                    castGfx = castGfx,
                    projectileGfx = projectileGfx,
                    impactGfx = impactGfx,
                    castAnim = castAnim,
                )
            }
            index = -1
            name = ""
            requiredLevel = -1
            baseDamage = -1
            spellId = -1
            buttonId = -1
            coolDown = -1
            castGfx = null
            projectileGfx = null
            impactGfx = null
            castAnim = 1979
            inEntry = false
            hasIndex = false
            hasSpellId = false
        }

        for (line in Files.readAllLines(file)) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue

            if (trimmed == "[[spell]]") {
                flush()
                inEntry = true
                continue
            }

            if (inEntry && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim().replace("#.*".toRegex(), "").trim().trim('"')
                if (value.isEmpty()) continue

                try {
                    when (key) {
                        "index" -> {
                            index = value.toInt()
                            hasIndex = true
                        }
                        "name" -> name = value
                        "requiredLevel" -> requiredLevel = value.toInt()
                        "baseDamage" -> baseDamage = value.toInt()
                        "spellId" -> {
                            spellId = value.toInt()
                            hasSpellId = true
                        }
                        "buttonId" -> buttonId = value.toInt()
                        "coolDown" -> coolDown = value.toInt()
                        "castGfx" -> castGfx = value
                        "projectileGfx" -> projectileGfx = value
                        "impactGfx" -> impactGfx = value
                        "castAnim" -> castAnim = value.toInt()
                    }
                } catch (_: NumberFormatException) {
                    // Ignore malformed fields
                }
            }
        }
        flush()
        return entries
    }
}

// Data is static, so every Client shares these cached arrays instead of each
// player re-allocating its own copy of the old hardcoded literals.
object AncientSpellRegistry {
    private val spells: List<AncientSpellDef> by lazy { TomlAncientSpellLoader.load().sortedBy { it.index } }

    private val byIndex: Map<Int, AncientSpellDef> by lazy { spells.associateBy { it.index } }

    fun gfx(index: Int): AncientSpellDef? = byIndex[index]

    @JvmStatic
    fun requiredLevel(): IntArray = requiredLevelCache

    @JvmStatic
    fun baseDamage(): IntArray = baseDamageCache

    @JvmStatic
    fun spellName(): Array<String> = spellNameCache

    @JvmStatic
    fun ancientId(): IntArray = ancientIdCache

    @JvmStatic
    fun ancientButton(): IntArray = ancientButtonCache

    /** Per-element cooldown, indexed by `spellIndex % 4` (smoke/shadow/blood/ice). */
    @JvmStatic
    fun coolDown(): IntArray = coolDownCache

    private val requiredLevelCache: IntArray by lazy { spells.map { it.requiredLevel }.toIntArray() }
    private val baseDamageCache: IntArray by lazy { spells.map { it.baseDamage }.toIntArray() }
    private val spellNameCache: Array<String> by lazy { spells.map { it.name }.toTypedArray() }
    private val ancientIdCache: IntArray by lazy { spells.map { it.spellId }.toIntArray() }
    private val ancientButtonCache: IntArray by lazy { spells.map { it.buttonId }.toIntArray() }
    private val coolDownCache: IntArray by lazy { spells.take(4).map { it.coolDown }.toIntArray() }
}
