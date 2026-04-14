package net.dodian.uber.game.model.objects

/**
 * Canonical runtime world object model.
 *
 * This shape is plane-aware and is used for temporary/global runtime spawns, interaction
 * distance checks, startup/static object overlays, and any flow that must preserve an explicit
 * z level.
 */
class WorldObject @JvmOverloads constructor(
    @JvmField var id: Int,
    @JvmField var x: Int,
    @JvmField var y: Int,
    @JvmField var z: Int,
    @JvmField var type: Int,
    @JvmField var face: Int = 0,
    @JvmField var oldId: Int = 0,
) {
    private var attachment: Any? = null

    fun setAttachment(attachment: Any?) {
        this.attachment = attachment
    }

    fun getAttachment(): Any? = attachment
}


