package net.dodian.uber.game.engine.systems.net

enum class PacketRejectReason(val wire: String) {
    SHORT_PAYLOAD("short_payload"),
    MALFORMED_PAYLOAD("malformed_payload"),
    INVALID_COORDINATE("invalid_coordinate"),
    INVALID_SLOT("invalid_slot"),
    INVALID_ID("invalid_id"),
    UNKNOWN_NPC("unknown_npc"),
    OPCODE_DISABLED("opcode_disabled"),
    LISTENER_EXCEPTION("listener_exception"),
}
