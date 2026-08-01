package net.dodian.uber.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

enum class SkillModuleKind { GAMEPLAY, SUPPORT }

abstract class SkillModuleExtension {
    abstract val moduleId: Property<String>
    abstract val implementationClass: Property<String>
    abstract val kind: Property<SkillModuleKind>
}

abstract class GenerateSkillModuleDescriptor : DefaultTask() {
    @get:Input abstract val moduleId: Property<String>
    @get:Input abstract val implementationClass: Property<String>
    @get:Input abstract val kind: Property<SkillModuleKind>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val id = moduleId.get()
        require(Regex("skill\\.[a-z][a-z0-9-]{1,63}").matches(id)) { "Invalid skill module id '$id'" }
        val output = outputDirectory.file("META-INF/ub3r/skill-modules/$id.toml").get().asFile
        ContentModuleBuildSupport.writeDescriptor(
            output,
            linkedMapOf(
                "schema_version" to "1",
                "module_id" to id,
                "implementation_class" to implementationClass.get(),
                "kind" to kind.get().name.lowercase(),
            ),
        )
    }
}

abstract class VerifySkillModule : DefaultTask() {
    @get:Input abstract val moduleId: Property<String>
    @get:Input abstract val implementationClass: Property<String>
    @get:Input abstract val kind: Property<SkillModuleKind>

    @TaskAction
    fun verify() {
        val projectRoot = project.projectDir
        val module = ContentModuleBuildSupport.readSources(projectRoot)
        val source = module.source
        ContentModuleBuildSupport.requireRuntimeTests(project.path, module.tests)
        check(
            !source.contains("project(\":game-server\")") &&
                !source.contains("net.dodian.uber.game.model.entity.player.Client") &&
                !source.contains("SkillEngineAccess"),
        ) {
            "${project.path} must remain independent of :game-server, Client, and SkillEngineAccess."
        }
        check(source.contains(implementationClass.get().substringAfterLast('.'))) {
            "${project.path} does not contain ${implementationClass.get()}."
        }
        val requiredContract = if (kind.get() == SkillModuleKind.GAMEPLAY) "SkillPlugin" else "SkillSupportModule"
        check(source.contains(requiredContract)) {
            "${project.path} ${implementationClass.get()} must implement $requiredContract."
        }
        if (kind.get() == SkillModuleKind.GAMEPLAY) {
            val routeRegistration = Regex("""\b(objectClick|npcClick|itemClick|itemOnItem|itemOnObject|magicOnObject|button|gatheringSpots|objectOption|npcOption)\s*\(""")
            check(routeRegistration.containsMatchIn(source)) {
                "${project.path} is descriptor-only: gameplay modules must register at least one route."
            }
        }
        check(module.resources.walkTopDown().any { it.isFile && it.extension == "toml" }) {
            "${project.path} must bundle at least one validated skill TOML file."
        }
    }
}

class Ub3rSkillPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("java-library")

        val extension = extensions.create<SkillModuleExtension>("skillModule")
        extension.moduleId.convention("skill.${project.name}")
        extension.kind.convention(SkillModuleKind.GAMEPLAY)

        dependencies {
            add("implementation", project(":skills:api"))
            add("implementation", project(":skills:runtime"))
            add("testImplementation", project(":skills:testkit"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter-api:5.9.3")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.9.3")
        }

        extensions.getByType<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }

        tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
        tasks.withType<KotlinCompile>().configureEach { kotlinOptions.jvmTarget = "21" }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }

        val generate = tasks.register<GenerateSkillModuleDescriptor>("generateSkillModuleDescriptor") {
            moduleId.set(extension.moduleId)
            implementationClass.set(extension.implementationClass)
            kind.set(extension.kind)
            outputDirectory.set(layout.buildDirectory.dir("generated/skill-module-descriptor"))
        }
        extensions.getByType<org.gradle.api.plugins.JavaPluginExtension>().sourceSets.named("main") {
            resources.srcDir(generate.map { it.outputDirectory })
        }
        tasks.withType<ProcessResources>().configureEach { dependsOn(generate) }

        val verify = tasks.register<VerifySkillModule>("verifySkillModule") {
            moduleId.set(extension.moduleId)
            implementationClass.set(extension.implementationClass)
            kind.set(extension.kind)
            dependsOn("test")
        }
        tasks.named("check") { dependsOn(verify) }
        }
    }
}
