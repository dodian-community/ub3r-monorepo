package net.dodian.uber.game.plugin.processor

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile

private data class DiscoveredSymbol(
    val packageName: String,
    val objectName: String,
) {
    val fqcn: String = "$packageName.$objectName"
}

class PluginModuleIndexSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    private var generated = false

    override fun process(resolver: Resolver): List<com.google.devtools.ksp.symbol.KSAnnotated> {
        if (generated) {
            return emptyList()
        }

        val allDeclarations = resolver.getAllFiles().flatMap { file ->
            file.declarations.filterIsInstance<KSClassDeclaration>()
                .map { file to it }
        }.toList()
        val allObjects = allDeclarations.filter { (_, declaration) -> declaration.classKind == ClassKind.OBJECT }

        validateSingletonContracts(allDeclarations)

        val interfaceButtons = discoverInterfaceButtons(allObjects)
        val objectContents = discoverObjectsByInterface(allObjects, "net.dodian.uber.game.content.objects.ObjectContent")
        val itemContents = discoverObjectsByInterface(allObjects, "net.dodian.uber.game.content.items.ItemContent")
        val commandContents = discoverCommandContents(allObjects)
        val npcModules = discoverNpcModules(allObjects)
        val skillPlugins = discoverSkillPlugins(allObjects)
        val eventBootstraps = discoverEventBootstraps(allObjects)
        val contentBootstraps = discoverContentBootstraps(allObjects)

        if (
            interfaceButtons.isEmpty() &&
            objectContents.isEmpty() &&
            itemContents.isEmpty() &&
            commandContents.isEmpty() &&
            npcModules.isEmpty() &&
            skillPlugins.isEmpty() &&
            eventBootstraps.isEmpty() &&
            contentBootstraps.isEmpty()
        ) {
            return emptyList()
        }

        validateUniqueObjectModuleNames(objectContents)

        val output = buildOutput(interfaceButtons, objectContents, itemContents, commandContents, npcModules, skillPlugins, eventBootstraps, contentBootstraps)
        val outputFile =
            codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = true, *resolver.getAllFiles().toList().toTypedArray()),
                packageName = "net.dodian.uber.game.plugin",
                fileName = "GeneratedPluginModuleIndex",
            )
        outputFile.bufferedWriter().use { it.write(output) }
        generated = true
        logger.info("Generated PluginModuleIndex with ${interfaceButtons.size} interface buttons, ${objectContents.size} object modules, ${itemContents.size} item modules, ${commandContents.size} command modules, ${npcModules.size} npc modules, ${skillPlugins.size} skill plugins, ${eventBootstraps.size} event bootstraps, ${contentBootstraps.size} content bootstraps.")
        return emptyList()
    }

    private fun validateSingletonContracts(allDeclarations: List<Pair<KSFile, KSClassDeclaration>>) {
        val violations = mutableListOf<String>()

        allDeclarations.forEach { (file, declaration) ->
            if (declaration.classKind == ClassKind.OBJECT) {
                return@forEach
            }
            val packageName = file.packageName.asString()
            val simpleName = declaration.simpleName.asString()

            fun recordViolation(message: String) {
                logger.error(message, declaration)
                violations += "$packageName.$simpleName"
            }

            if (
                (
                    packageName.startsWith("net.dodian.uber.game.content.interfaces") ||
                        packageName.startsWith("net.dodian.uber.game.content.ui")
                    ) &&
                declaration.implementsInterface("net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent")
            ) {
                recordViolation("Interface button content must be declared as Kotlin 'object': $packageName.$simpleName")
            }

            if (
                packageName.startsWith("net.dodian.uber.game.content") &&
                declaration.implementsInterface("net.dodian.uber.game.content.objects.ObjectContent")
            ) {
                recordViolation("Object content must be declared as Kotlin 'object': $packageName.$simpleName")
            }

            if (
                packageName.startsWith("net.dodian.uber.game.content") &&
                declaration.implementsInterface("net.dodian.uber.game.content.items.ItemContent")
            ) {
                recordViolation("Item content must be declared as Kotlin 'object': $packageName.$simpleName")
            }

            if (
                packageName.startsWith("net.dodian.uber.game.content.commands") &&
                declaration.implementsInterface("net.dodian.uber.game.systems.dispatch.commands.CommandContent")
            ) {
                recordViolation("Command content must be declared as Kotlin 'object': $packageName.$simpleName")
            }

            if (
                packageName.startsWith("net.dodian.uber.game.content.npcs") &&
                declaration.implementsInterface("net.dodian.uber.game.content.npcs.NpcModule")
            ) {
                recordViolation("NPC module must be declared as Kotlin 'object': $packageName.$simpleName")
            }

            if (
                packageName.startsWith("net.dodian.uber.game.content.skills") &&
                declaration.implementsInterface("net.dodian.uber.game.systems.skills.plugin.SkillPlugin")
            ) {
                recordViolation("Skill plugin must be declared as Kotlin 'object': $packageName.$simpleName")
            }

            if (
                (packageName.startsWith("net.dodian.uber.game.systems.dispatch") ||
                    packageName.startsWith("net.dodian.uber.game.systems.skills")) &&
                declaration.implementsInterface("net.dodian.uber.game.systems.dispatch.ContentBootstrap")
            ) {
                recordViolation("Content bootstrap must be declared as Kotlin 'object': $packageName.$simpleName")
            }
        }

        if (violations.isNotEmpty()) {
            throw IllegalStateException(
                "KSP module discovery failed: ${violations.size} non-singleton module declarations found.",
            )
        }
    }

    private fun validateUniqueObjectModuleNames(objectContents: List<DiscoveredSymbol>) {
        val duplicates = objectContents.groupBy { it.objectName }.filterValues { it.size > 1 }
        if (duplicates.isEmpty()) {
            return
        }

        duplicates.forEach { (objectName, symbols) ->
            val fqcnList = symbols.joinToString(", ") { it.fqcn }
            logger.error(
                "Duplicate ObjectContent module name '$objectName' detected. " +
                    "Generated index key collisions are not allowed. Modules: $fqcnList",
            )
        }

        throw IllegalStateException(
            "KSP module discovery failed: duplicate ObjectContent module names detected (${duplicates.size}).",
        )
    }

    private fun discoverInterfaceButtons(allObjects: List<Pair<KSFile, KSClassDeclaration>>): List<DiscoveredSymbol> {
        val interfaceButtonsType = "net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent"
        return allObjects
            .filter { (file, declaration) ->
                (
                    file.packageName.asString().startsWith("net.dodian.uber.game.content.interfaces") ||
                        file.packageName.asString().startsWith("net.dodian.uber.game.content.ui")
                    ) &&
                    declaration.implementsInterface(interfaceButtonsType)
            }
            .map { (_, declaration) -> declaration.toDiscoveredSymbol() }
            .sortedBy { it.fqcn }
    }

    private fun discoverObjectsByInterface(
        allObjects: List<Pair<KSFile, KSClassDeclaration>>,
        interfaceFqcn: String,
    ): List<DiscoveredSymbol> {
        return allObjects
            .mapNotNull { (_, declaration) ->
                if (declaration.implementsInterface(interfaceFqcn)) {
                    declaration.toDiscoveredSymbol()
                } else {
                    null
                }
            }
            .sortedBy { it.fqcn }
    }

    private fun discoverNpcModules(allObjects: List<Pair<KSFile, KSClassDeclaration>>): List<DiscoveredSymbol> {
        val npcModuleType = "net.dodian.uber.game.content.npcs.NpcModule"
        return allObjects
            .filter { (file, declaration) ->
                file.packageName.asString().startsWith("net.dodian.uber.game.content.npcs") &&
                    declaration.implementsInterface(npcModuleType)
            }
            .map { (_, declaration) -> declaration.toDiscoveredSymbol() }
            .sortedBy { it.fqcn }
    }

    private fun discoverSkillPlugins(allObjects: List<Pair<KSFile, KSClassDeclaration>>): List<DiscoveredSymbol> {
        val skillPluginType = "net.dodian.uber.game.systems.skills.plugin.SkillPlugin"
        return allObjects
            .filter { (file, declaration) ->
                file.packageName.asString().startsWith("net.dodian.uber.game.content.skills") &&
                    declaration.implementsInterface(skillPluginType)
            }
            .map { (_, declaration) -> declaration.toDiscoveredSymbol() }
            .sortedBy { it.fqcn }
    }

    private fun discoverCommandContents(allObjects: List<Pair<KSFile, KSClassDeclaration>>): List<DiscoveredSymbol> {
        val commandContentType = "net.dodian.uber.game.systems.dispatch.commands.CommandContent"
        return allObjects
            .filter { (file, declaration) ->
                file.packageName.asString().startsWith("net.dodian.uber.game.content.commands") &&
                    declaration.implementsInterface(commandContentType)
            }
            .map { (_, declaration) -> declaration.toDiscoveredSymbol() }
            .sortedBy { it.fqcn }
    }

    private fun discoverEventBootstraps(allObjects: List<Pair<KSFile, KSClassDeclaration>>): List<DiscoveredSymbol> {
        return allObjects
            .filter { (file, declaration) ->
                file.packageName.asString().startsWith("net.dodian.uber.game.engine.event.bootstrap") &&
                    declaration.simpleName.asString().endsWith("Bootstrap") &&
                    declaration.simpleName.asString() != "CoreEventBusBootstrap"
            }
            .map { (_, declaration) -> declaration.toDiscoveredSymbol() }
            .sortedBy { it.fqcn }
    }

    private fun discoverContentBootstraps(allObjects: List<Pair<KSFile, KSClassDeclaration>>): List<DiscoveredSymbol> {
        val bootstrapType = "net.dodian.uber.game.systems.dispatch.ContentBootstrap"
        return allObjects
            .filter { (file, declaration) ->
                (file.packageName.asString().startsWith("net.dodian.uber.game.systems.dispatch") ||
                    file.packageName.asString().startsWith("net.dodian.uber.game.systems.skills")) &&
                    declaration.implementsInterface(bootstrapType)
            }
            .map { (_, declaration) -> declaration.toDiscoveredSymbol() }
            .sortedBy { it.fqcn }
    }

    private fun KSClassDeclaration.implementsInterface(interfaceFqcn: String): Boolean {
        return getAllSuperTypes().any { superType ->
            superType.declaration.qualifiedName?.asString() == interfaceFqcn
        }
    }

    private fun KSClassDeclaration.toDiscoveredSymbol(): DiscoveredSymbol {
        val packageName = packageName.asString()
        val objectName = simpleName.asString()
        return DiscoveredSymbol(packageName = packageName, objectName = objectName)
    }

    private fun buildOutput(
        interfaceButtons: List<DiscoveredSymbol>,
        objectContents: List<DiscoveredSymbol>,
        itemContents: List<DiscoveredSymbol>,
        commandContents: List<DiscoveredSymbol>,
        npcModules: List<DiscoveredSymbol>,
        skillPlugins: List<DiscoveredSymbol>,
        eventBootstraps: List<DiscoveredSymbol>,
        contentBootstraps: List<DiscoveredSymbol>,
    ): String {
        val out = StringBuilder()
        out.appendLine("package net.dodian.uber.game.plugin")
        out.appendLine()
        out.appendLine("import net.dodian.uber.game.systems.dispatch.ContentBootstrap")
        out.appendLine("import net.dodian.uber.game.systems.dispatch.commands.CommandContent")
        out.appendLine("import net.dodian.uber.game.content.items.ItemContent")
        out.appendLine("import net.dodian.uber.game.content.npcs.NpcContentDefinition")
        out.appendLine("import net.dodian.uber.game.content.objects.ObjectContent")
        out.appendLine("import net.dodian.uber.game.systems.skills.plugin.SkillPlugin")
        out.appendLine("import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent")
        out.appendLine()
        out.appendLine("object GeneratedPluginModuleIndex {")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val interfaceButtons: List<InterfaceButtonContent> = listOf(")
        if (interfaceButtons.isNotEmpty()) {
            interfaceButtons.forEachIndexed { index, symbol ->
                val suffix = if (index == interfaceButtons.lastIndex) "" else ","
                out.appendLine("        ${symbol.fqcn}$suffix")
            }
        }
        out.appendLine("    )")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val objectContents: List<Pair<String, ObjectContent>> = listOf(")
        if (objectContents.isNotEmpty()) {
            objectContents.forEachIndexed { index, symbol ->
                val suffix = if (index == objectContents.lastIndex) "" else ","
                out.appendLine("        \"${symbol.objectName}\" to ${symbol.fqcn}$suffix")
            }
        }
        out.appendLine("    )")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val itemContents: List<ItemContent> = listOf(")
        if (itemContents.isNotEmpty()) {
            itemContents.forEachIndexed { index, symbol ->
                val suffix = if (index == itemContents.lastIndex) "" else ","
                out.appendLine("        ${symbol.fqcn}$suffix")
            }
        }
        out.appendLine("    )")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val commandContents: List<CommandContent> = listOf(")
        if (commandContents.isNotEmpty()) {
            commandContents.forEachIndexed { index, symbol ->
                val suffix = if (index == commandContents.lastIndex) "" else ","
                out.appendLine("        ${symbol.fqcn}$suffix")
            }
        }
        out.appendLine("    )")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val npcContents: List<NpcContentDefinition> = listOf(")
        if (npcModules.isNotEmpty()) {
            npcModules.forEachIndexed { index, symbol ->
                val suffix = if (index == npcModules.lastIndex) "" else ","
                out.appendLine("        ${symbol.fqcn}.definition$suffix")
            }
        }
        out.appendLine("    )")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val skillPlugins: List<SkillPlugin> = listOf(")
        if (skillPlugins.isNotEmpty()) {
            skillPlugins.forEachIndexed { index, symbol ->
                val suffix = if (index == skillPlugins.lastIndex) "" else ","
                out.appendLine("        ${symbol.fqcn}$suffix")
            }
        }
        out.appendLine("    )")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val eventBootstraps: List<() -> Unit> = listOf(")
        if (eventBootstraps.isNotEmpty()) {
            eventBootstraps.forEachIndexed { index, symbol ->
                val suffix = if (index == eventBootstraps.lastIndex) "" else ","
                out.appendLine("        { ${symbol.fqcn}.bootstrap() }$suffix")
            }
        }
        out.appendLine("    )")
        out.appendLine()

        out.appendLine("    @JvmField")
        out.appendLine("    val contentBootstraps: List<ContentBootstrap> = listOf(")
        if (contentBootstraps.isNotEmpty()) {
            contentBootstraps.forEachIndexed { index, symbol ->
                val suffix = if (index == contentBootstraps.lastIndex) "" else ","
                out.appendLine("        ${symbol.fqcn}$suffix")
            }
        }
        out.appendLine("    )")

        out.appendLine("}")
        return out.toString()
    }
}
