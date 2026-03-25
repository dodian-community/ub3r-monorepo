package net.dodian.uber.game.plugin.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal class GeneratedPluginModuleIndexWriter(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) {
    private data class EventBootstrapEntry(val moduleFqcn: String, val function: String)
    private data class NpcEntry(val moduleFqcn: String, val explicitName: String, val ownsSpawnDefinitions: Boolean)

    fun generate(resolver: Resolver) {
        val interfaceButtons = collectModuleFqcns(
            resolver,
            "net.dodian.uber.game.plugin.annotations.RegisterInterfaceButtons",
            requiredSupertypeFqcn = "net.dodian.uber.game.ui.buttons.InterfaceButtonContent",
        )
        val objectContents = collectModuleFqcns(
            resolver,
            "net.dodian.uber.game.plugin.annotations.RegisterObjectContent",
            requiredSupertypeFqcn = "net.dodian.uber.game.content.objects.ObjectContent",
        )
        val itemContents = collectModuleFqcns(
            resolver,
            "net.dodian.uber.game.plugin.annotations.RegisterItemContent",
            requiredSupertypeFqcn = "net.dodian.uber.game.content.items.ItemContent",
        )
        val eventBootstraps = collectEventBootstraps(resolver)
        val npcContents = collectNpcModules(resolver)

        val dependencies = Dependencies(true, *resolver.getAllFiles().toList().toTypedArray())

        codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = "net.dodian.uber.game.plugin",
            fileName = "GeneratedPluginModuleIndex",
        ).bufferedWriter().use { out ->
            out.appendLine("package net.dodian.uber.game.plugin")
            out.appendLine()
            out.appendLine("import net.dodian.uber.game.content.items.ItemContent")
            out.appendLine("import net.dodian.uber.game.content.npc.NpcModuleDefinitionBuilder")
            out.appendLine("import net.dodian.uber.game.content.npc.NpcContentDefinition")
            out.appendLine("import net.dodian.uber.game.content.objects.ObjectContent")
            out.appendLine("import net.dodian.uber.game.ui.buttons.InterfaceButtonContent")
            out.appendLine()
            out.appendLine("object GeneratedPluginModuleIndex {")

            out.appendLine("    @JvmField")
            out.appendLine("    val interfaceButtons: List<InterfaceButtonContent> = listOf(")
            interfaceButtons.forEachIndexed { index, fqcn ->
                out.append("        ").append(fqcn)
                if (index != interfaceButtons.lastIndex) out.append(",")
                out.appendLine()
            }
            out.appendLine("    )")
            out.appendLine()

            out.appendLine("    @JvmField")
            out.appendLine("    val objectContents: List<Pair<String, ObjectContent>> = listOf(")
            objectContents.forEachIndexed { index, fqcn ->
                val name = fqcn.substringAfterLast('.')
                out.append("        \"").append(name).append("\" to ").append(fqcn)
                if (index != objectContents.lastIndex) out.append(",")
                out.appendLine()
            }
            out.appendLine("    )")
            out.appendLine()

            out.appendLine("    @JvmField")
            out.appendLine("    val itemContents: List<ItemContent> = listOf(")
            itemContents.forEachIndexed { index, fqcn ->
                out.append("        ").append(fqcn)
                if (index != itemContents.lastIndex) out.append(",")
                out.appendLine()
            }
            out.appendLine("    )")
            out.appendLine()

            out.appendLine("    @JvmField")
            out.appendLine("    val npcContents: List<NpcContentDefinition> = listOf(")
            npcContents.forEachIndexed { index, entry ->
                out.append("        NpcModuleDefinitionBuilder.fromModule(")
                    .append("module = ").append(entry.moduleFqcn)
                    .append(", explicitName = \"").append(escape(entry.explicitName)).append("\"")
                    .append(", ownsSpawnDefinitions = ").append(entry.ownsSpawnDefinitions.toString())
                    .append(")")
                if (index != npcContents.lastIndex) out.append(",")
                out.appendLine()
            }
            out.appendLine("    )")
            out.appendLine()

            out.appendLine("    @JvmField")
            out.appendLine("    val eventBootstraps: List<() -> Unit> = listOf(")
            eventBootstraps.forEachIndexed { index, entry ->
                out.append("        { ").append(entry.moduleFqcn).append(".").append(entry.function).append("() }")
                if (index != eventBootstraps.lastIndex) out.append(",")
                out.appendLine()
            }
            out.appendLine("    )")

            out.appendLine("}")
        }
    }

    private fun collectModuleFqcns(
        resolver: Resolver,
        annotationFqcn: String,
        requiredSupertypeFqcn: String,
    ): List<String> {
        val modules = mutableListOf<String>()
        val requiredType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(requiredSupertypeFqcn))?.asType(emptyList())
        for (symbol in resolver.getSymbolsWithAnnotation(annotationFqcn)) {
            if (symbol !is KSClassDeclaration) continue
            for (annotation in symbol.annotations) {
                if (annotation.annotationType.resolve().declaration.qualifiedName?.asString() != annotationFqcn) continue
                val moduleType = annotation.arguments.firstOrNull { it.name?.asString() == "module" }?.value as? KSType
                if (moduleType == null) {
                    logger.error("Missing module argument for $annotationFqcn", symbol)
                    continue
                }
                val moduleDecl = moduleType.declaration as? KSClassDeclaration ?: continue
                validateObject(moduleDecl, annotationFqcn)
                if (requiredType != null) {
                    val moduleAsType = moduleDecl.asType(emptyList())
                    if (!requiredType.isAssignableFrom(moduleAsType)) {
                        logger.error(
                            "$annotationFqcn module ${moduleDecl.qualifiedName!!.asString()} must implement $requiredSupertypeFqcn",
                            moduleDecl,
                        )
                    }
                }
                modules += moduleDecl.qualifiedName!!.asString()
            }
        }
        return sortedDistinct(modules, annotationFqcn)
    }

    private fun collectEventBootstraps(resolver: Resolver): List<EventBootstrapEntry> {
        val annotationFqcn = "net.dodian.uber.game.plugin.annotations.RegisterEventBootstrap"
        val modules = mutableListOf<EventBootstrapEntry>()
        for (symbol in resolver.getSymbolsWithAnnotation(annotationFqcn)) {
            if (symbol !is KSClassDeclaration) continue
            for (annotation in symbol.annotations) {
                if (annotation.annotationType.resolve().declaration.qualifiedName?.asString() != annotationFqcn) continue
                val moduleType = annotation.arguments.firstOrNull { it.name?.asString() == "module" }?.value as? KSType
                val function = annotation.arguments.firstOrNull { it.name?.asString() == "function" }?.value as? String ?: "bootstrap"
                if (moduleType == null) {
                    logger.error("Missing module argument for $annotationFqcn", symbol)
                    continue
                }
                val moduleDecl = moduleType.declaration as? KSClassDeclaration ?: continue
                validateObject(moduleDecl, annotationFqcn)
                modules += EventBootstrapEntry(moduleDecl.qualifiedName!!.asString(), function)
            }
        }
        val duplicateKeys = modules.groupBy { "${it.moduleFqcn}#${it.function}" }.filterValues { it.size > 1 }.keys.sorted()
        if (duplicateKeys.isNotEmpty()) {
            logger.error("Duplicate $annotationFqcn modules: ${duplicateKeys.joinToString(", ")}")
        }
        return modules.distinctBy { "${it.moduleFqcn}#${it.function}" }.sortedBy { "${it.moduleFqcn}#${it.function}" }
    }

    private fun collectNpcModules(resolver: Resolver): List<NpcEntry> {
        val annotationFqcn = "net.dodian.uber.game.plugin.annotations.RegisterNpcContent"
        val modules = mutableListOf<NpcEntry>()
        for (symbol in resolver.getSymbolsWithAnnotation(annotationFqcn)) {
            if (symbol !is KSClassDeclaration) continue
            for (annotation in symbol.annotations) {
                if (annotation.annotationType.resolve().declaration.qualifiedName?.asString() != annotationFqcn) continue
                val moduleType = annotation.arguments.firstOrNull { it.name?.asString() == "module" }?.value as? KSType
                val owns = annotation.arguments.firstOrNull { it.name?.asString() == "ownsSpawnDefinitions" }?.value as? Boolean ?: false
                val explicitName = annotation.arguments.firstOrNull { it.name?.asString() == "explicitName" }?.value as? String ?: ""
                if (moduleType == null) {
                    logger.error("Missing module argument for $annotationFqcn", symbol)
                    continue
                }
                val moduleDecl = moduleType.declaration as? KSClassDeclaration ?: continue
                validateObject(moduleDecl, annotationFqcn)
                val properties = moduleDecl.getAllProperties().map { it.simpleName.asString() }.toSet()
                val hasNpcIds = properties.contains("npcIds")
                val hasDslDefinition = properties.contains("definition") || properties.contains("plugin")
                if (!hasNpcIds && !hasDslDefinition) {
                    logger.error(
                        "Module ${moduleDecl.qualifiedName!!.asString()} must expose npcIds or definition/plugin property",
                        moduleDecl,
                    )
                }
                modules += NpcEntry(moduleDecl.qualifiedName!!.asString(), explicitName, owns)
            }
        }
        val duplicateKeys = modules.groupBy { it.moduleFqcn }.filterValues { it.size > 1 }.keys.sorted()
        if (duplicateKeys.isNotEmpty()) {
            logger.error("Duplicate $annotationFqcn modules: ${duplicateKeys.joinToString(", ")}")
        }
        return modules.distinctBy { it.moduleFqcn }.sortedBy { it.moduleFqcn }
    }

    private fun validateObject(moduleDecl: KSClassDeclaration, annotationFqcn: String) {
        if (moduleDecl.classKind != ClassKind.OBJECT) {
            logger.error("$annotationFqcn requires Kotlin object target, got ${moduleDecl.classKind} (${moduleDecl.qualifiedName?.asString()})")
        }
    }

    private fun sortedDistinct(values: List<String>, annotationFqcn: String): List<String> {
        val duplicates = values.groupBy { it }.filterValues { it.size > 1 }.keys.sorted()
        if (duplicates.isNotEmpty()) {
            logger.error("Duplicate $annotationFqcn modules: ${duplicates.joinToString(", ")}")
        }
        return values.distinct().sorted()
    }

    private fun escape(input: String): String =
        input.replace("\\", "\\\\").replace("\"", "\\\"")
}
