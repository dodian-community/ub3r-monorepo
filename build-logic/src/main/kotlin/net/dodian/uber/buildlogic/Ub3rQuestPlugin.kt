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

abstract class QuestModuleExtension {
    abstract val moduleId: Property<String>
    abstract val implementationClass: Property<String>
}

abstract class GenerateQuestModuleDescriptor : DefaultTask() {
    @get:Input abstract val moduleId: Property<String>
    @get:Input abstract val implementationClass: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val id = moduleId.get()
        require(Regex("quest\\.[a-z][a-z0-9-]{1,63}").matches(id)) { "Invalid quest module id '$id'" }
        val output = outputDirectory.file("META-INF/ub3r/quest-modules/$id.toml").get().asFile
        ContentModuleBuildSupport.writeDescriptor(
            output,
            linkedMapOf(
                "schema_version" to "1",
                "module_id" to id,
                "implementation_class" to implementationClass.get(),
            ),
        )
    }
}

abstract class VerifyQuestModule : DefaultTask() {
    @get:Input abstract val moduleId: Property<String>
    @get:Input abstract val implementationClass: Property<String>

    @TaskAction
    fun verify() {
        val projectRoot = project.projectDir
        val module = ContentModuleBuildSupport.readSources(projectRoot)
        val source = module.source
        ContentModuleBuildSupport.requireRuntimeTests(project.path, module.tests)
        check(
            !source.contains("project(\":game-server\")") &&
                !source.contains("net.dodian.uber.game.model.entity.player.Client"),
        ) {
            "${project.path} must remain independent of :game-server and Client."
        }
        check(source.contains(implementationClass.get().substringAfterLast('.'))) {
            "${project.path} does not contain ${implementationClass.get()}."
        }
        check(source.contains("QuestPlugin")) {
            "${project.path} ${implementationClass.get()} must implement QuestPlugin."
        }
        val routeRegistration = Regex("""\b(objectClick|npcClick|itemOnItem|itemClick|itemOnObject|magicOnObject|button)\s*\(""")
        check(routeRegistration.containsMatchIn(source)) {
            "${project.path} is descriptor-only: quest modules must register at least one route."
        }
    }
}

class Ub3rQuestPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("java-library")

        val extension = extensions.create<QuestModuleExtension>("questModule")
        extension.moduleId.convention("quest.${project.name}")

        dependencies {
            add("implementation", project(":quests:api"))
            add("implementation", project(":quests:runtime"))
            add("testImplementation", project(":quests:testkit"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter-api:5.9.3")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.9.3")
        }

        extensions.getByType<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }

        tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
        tasks.withType<KotlinCompile>().configureEach { kotlinOptions.jvmTarget = "21" }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }

        val generate = tasks.register<GenerateQuestModuleDescriptor>("generateQuestModuleDescriptor") {
            moduleId.set(extension.moduleId)
            implementationClass.set(extension.implementationClass)
            outputDirectory.set(layout.buildDirectory.dir("generated/quest-module-descriptor"))
        }
        extensions.getByType<org.gradle.api.plugins.JavaPluginExtension>().sourceSets.named("main") {
            resources.srcDir(generate.map { it.outputDirectory })
        }
        tasks.withType<ProcessResources>().configureEach { dependsOn(generate) }

        val verify = tasks.register<VerifyQuestModule>("verifyQuestModule") {
            moduleId.set(extension.moduleId)
            implementationClass.set(extension.implementationClass)
            dependsOn("test")
        }
        tasks.named("check") { dependsOn(verify) }
        }
    }
}
