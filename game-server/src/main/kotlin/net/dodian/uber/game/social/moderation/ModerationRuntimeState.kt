package net.dodian.uber.game.social.moderation

/** Per-staff-member transient state for the moderation control panel. */
class ModerationRuntimeState {
    var dialogueStage: Int = 0
    var selectedTarget: String = ""
    val visiblePlayers = ArrayList<String>()

    fun clearDialogue() { dialogueStage = 0 }
    fun clear() { dialogueStage = 0; selectedTarget = ""; visiblePlayers.clear() }
}
