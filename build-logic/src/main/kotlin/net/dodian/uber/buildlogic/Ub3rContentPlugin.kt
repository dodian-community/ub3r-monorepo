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

abstract class ContentModuleExtension {
    abstract val moduleId: Property<String>
    abstract val implementationClass: Property<String>
    abstract val family: Property<String>
}

abstract class GenerateContentModuleDescriptor : DefaultTask() {
    @get:Input abstract val moduleId: Property<String>
    @get:Input abstract val implementationClass: Property<String>
    @get:Input abstract val family: Property<String>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val id = moduleId.get()
        require(Regex("[a-z][a-z0-9_.-]{2,127}").matches(id)) { "Invalid content module id '$id'" }
        val output = outputDirectory.file("META-INF/ub3r/content-modules/$id.toml").get().asFile
        ContentModuleBuildSupport.writeDescriptor(
            output,
            linkedMapOf(
                "schema_version" to "1",
                "module_id" to id,
                "implementation_class" to implementationClass.get(),
                "family" to family.get(),
            ),
        )
    }
}

abstract class VerifyContentModule : DefaultTask() {
    @get:Input abstract val implementationClass: Property<String>

    @TaskAction
    fun verify() {
        val module = ContentModuleBuildSupport.readSources(project.projectDir)
        ContentModuleBuildSupport.requireRuntimeTests(project.path, module.tests)
        check(!module.source.contains("project(\":game-server\")") &&
            !module.source.contains("net.dodian.uber.game.model.entity.player.Client")) {
            "${project.path} must remain engine-independent."
        }
        check(module.source.contains(implementationClass.get().substringAfterLast('.'))) {
            "${project.path} does not contain ${implementationClass.get()}."
        }
    }
}

class Ub3rContentPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("java-library")
        val extension = extensions.create<ContentModuleExtension>("contentModule")
        extension.moduleId.convention("content.${project.name}")
        extension.family.convention("generic")
        dependencies {
            add("implementation", project(":content-platform:api"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter-api:5.9.3")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:5.9.3")
        }
        extensions.getByType<JavaPluginExtension>().toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        tasks.withType<JavaCompile>().configureEach { options.release.set(21) }
        tasks.withType<KotlinCompile>().configureEach { kotlinOptions.jvmTarget = "21" }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
        val generate = tasks.register<GenerateContentModuleDescriptor>("generateContentModuleDescriptor") {
            moduleId.set(extension.moduleId)
            implementationClass.set(extension.implementationClass)
            family.set(extension.family)
            outputDirectory.set(layout.buildDirectory.dir("generated/content-module-descriptor"))
        }
        extensions.getByType<org.gradle.api.plugins.JavaPluginExtension>().sourceSets.named("main") {
            resources.srcDir(generate.map { it.outputDirectory })
        }
        tasks.withType<ProcessResources>().configureEach { dependsOn(generate) }
        val verify = tasks.register<VerifyContentModule>("verifyContentModule") {
            implementationClass.set(extension.implementationClass)
            dependsOn("test")
        }
        tasks.named("check") { dependsOn(verify) }
        }
    }
}
