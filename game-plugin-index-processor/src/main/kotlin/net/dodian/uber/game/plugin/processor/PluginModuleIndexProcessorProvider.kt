package net.dodian.uber.game.plugin.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class PluginModuleIndexProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        PluginModuleIndexProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
}

private class PluginModuleIndexProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val delegate = GeneratedPluginModuleIndexWriter(codeGenerator, logger)
    private var generated = false

    override fun process(resolver: com.google.devtools.ksp.processing.Resolver): List<com.google.devtools.ksp.symbol.KSAnnotated> {
        if (generated) return emptyList()
        delegate.generate(resolver)
        generated = true
        return emptyList()
    }
}
