package net.dodian.uber.game.model.`object`

class RS2Object @JvmOverloads constructor(
    @JvmField var id: Int,
    @JvmField var x: Int,
    @JvmField var y: Int,
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
