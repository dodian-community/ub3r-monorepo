# KSP Plugin Processor (`ksp-processor`)

## What is it?
The `ksp-processor` is a custom Kotlin Symbol Processing (KSP) plugin. It acts as a compile-time code generator that discovers all content modules in the server and wires them together.

In older RSPS architectures, content (like a new quest or skill) had to be manually added to a giant `switch` statement or loaded via slow runtime reflection. The Ub3r server solves this by generating a static index of all content *before* the server ever runs.

## How it Works

1.  **Compilation Hook**: When the `game-server` module is compiled via Gradle, the `ksp-processor` intercepts the Kotlin compiler.
2.  **Symbol Scanning**: The `PluginModuleIndexSymbolProcessor` scans the Abstract Syntax Tree (AST) of the entire codebase.
3.  **Filtering**: It looks specifically for Kotlin `object` declarations (Singletons) that implement core engine interfaces.
4.  **Code Generation**: It generates a new Kotlin file: `net.dodian.uber.game.plugin.GeneratedPluginModuleIndex.kt`.

## Supported Plugin Types
The processor currently scans for and registers objects implementing the following interfaces:

*   `InterfaceButtonContent` (Buttons)
*   `ObjectContent` (World Objects)
*   `ItemContent` (Inventory Items)
*   `CommandContent` (Player/Admin Commands)
*   `NpcModule` (NPC Interactions)
*   `SkillPlugin` (Modular Skills)
*   `ShopPlugin` (Shops)
*   `ContentBootstrap` (Initialization logic)
*   Event Bootstraps (Objects ending with `Bootstrap` in the `event` package).

## Validation & Safety
The processor enforces several strict contracts at compile time, failing the build if violated:
1.  **Must be a Singleton**: All content modules *must* be declared as a Kotlin `object`. If a developer declares `class MyObject : ObjectContent`, the build will fail with: `"Object content must be declared as Kotlin 'object'"`.
2.  **Unique Naming**: For `ObjectContent`, the processor groups by the `objectName`. If two files declare an object with the same name, the build fails to prevent key collisions in the generated index.

## The Generated Artifact
The output is a hardcoded, lightning-fast static registry. For example:
```kotlin
object GeneratedPluginModuleIndex {
    @JvmField
    val skillPlugins: List<SkillPlugin> = listOf(
        net.dodian.uber.game.content.skills.WoodcuttingPlugin,
        net.dodian.uber.game.content.skills.MiningPlugin
    )
    // ... other registries
}
```
At runtime, `ContentModuleIndex.kt` simply references this generated class, meaning content discovery takes 0 milliseconds during server startup.