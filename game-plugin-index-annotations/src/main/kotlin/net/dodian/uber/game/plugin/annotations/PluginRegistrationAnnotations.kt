package net.dodian.uber.game.plugin.annotations

import kotlin.reflect.KClass

@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterInterfaceButtons(val module: KClass<*>)

@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterObjectContent(val module: KClass<*>)

@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterItemContent(val module: KClass<*>)

@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterNpcContent(
    val module: KClass<*>,
    val ownsSpawnDefinitions: Boolean = false,
    val explicitName: String = "",
)

@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RegisterEventBootstrap(
    val module: KClass<*>,
    val function: String = "bootstrap",
)
