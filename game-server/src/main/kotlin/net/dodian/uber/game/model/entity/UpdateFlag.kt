package net.dodian.uber.game.model.entity

import net.dodian.uber.game.model.entity.Entity

enum class UpdateFlag(
    private val bitPosition: Int,
) {
    FACE_CHARACTER(0),
    FACE_COORDINATE(1),
    CHAT(2),
    HIT(3),
    ANIM(4),
    FORCED_CHAT(5),
    HIT2(6),
    GRAPHICS(7),
    APPEARANCE(8),
    FORCED_MOVEMENT(9),
    DUMMY(10),
    ;

    private val mask: Int = 1 shl bitPosition
    private var playerMask: Int = mask
    private var npcMask: Int = mask

    fun getBitPosition(): Int = bitPosition

    fun getMask(): Int = mask

    fun getMask(type: Entity.Type): Int =
        when (type) {
            Entity.Type.PLAYER -> {
                check(playerMask != -1) { "Player mask not set for $this" }
                playerMask
            }

            Entity.Type.NPC -> {
                check(npcMask != -1) { "NPC mask not set for $this" }
                npcMask
            }
        }

    fun setMasks(playerMask: Int, npcMask: Int) {
        this.playerMask = playerMask
        this.npcMask = npcMask
    }

    companion object {
        init {
            FACE_CHARACTER.setMasks(0x1, 0x20)
            FACE_COORDINATE.setMasks(0x2, 0x4)
            CHAT.setMasks(0x80, -1)
            HIT.setMasks(0x20, 0x40)
            ANIM.setMasks(0x8, 0x10)
            FORCED_CHAT.setMasks(0x4, 0x1)
            HIT2.setMasks(0x200, 0x8)
            GRAPHICS.setMasks(0x100, 0x80)
            APPEARANCE.setMasks(0x10, 0x2)
            FORCED_MOVEMENT.setMasks(0x400, -1)
            DUMMY.setMasks(0, 0)
        }
    }
}
