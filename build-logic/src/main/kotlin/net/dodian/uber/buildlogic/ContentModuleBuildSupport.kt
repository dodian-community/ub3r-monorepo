package net.dodian.uber.buildlogic

import java.io.File

internal data class ContentModuleSources(
    val source: String,
    val tests: List<File>,
    val resources: File,
)

internal object ContentModuleBuildSupport {
    fun writeDescriptor(output: File, values: LinkedHashMap<String, String>) {
        output.parentFile.mkdirs()
        output.writeText(
            buildString {
                values.forEach { (key, value) -> appendLine("$key = \"$value\"") }
            },
        )
    }

    fun readSources(projectRoot: File): ContentModuleSources {
        val sourceRoot = projectRoot.resolve("src/main/kotlin")
        val testRoot = projectRoot.resolve("src/test/kotlin")
        return ContentModuleSources(
            source = sourceRoot.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .joinToString("\n") { it.readText() },
            tests = testRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList(),
            resources = projectRoot.resolve("src/main/resources"),
        )
    }

    fun requireRuntimeTests(projectPath: String, tests: List<File>) {
        check(tests.isNotEmpty() && tests.any { it.readText().contains("@Test") }) {
            "$projectPath must contain at least one JUnit runtime test."
        }
    }
}
