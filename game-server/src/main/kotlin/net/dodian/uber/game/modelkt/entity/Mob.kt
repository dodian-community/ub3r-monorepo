package net.dodian.uber.game.modelkt.entity

import net.dodian.uber.cache.definition.NpcDefinition
import net.dodian.uber.game.action.Action
import net.dodian.uber.game.modelkt.World
import net.dodian.uber.game.modelkt.area.Direction
import net.dodian.uber.game.modelkt.area.Position
import net.dodian.uber.game.modelkt.inventory.CAPACITY_EQUIPMENT
import net.dodian.uber.game.modelkt.inventory.Inventory
import net.dodian.uber.game.sync.block.InteractingMobBlock
import net.dodian.uber.game.sync.block.SynchronizationBlockSet

@Suppress("MemberVisibilityCanBePrivate")
abstract class Mob : Entity {
    abstract override val world: World
    abstract override var position: Position

    var isTeleporting: Boolean = false

    val walkingQueue: WalkingQueue = WalkingQueue(this)
    val equipment: Inventory = Inventory(CAPACITY_EQUIPMENT, Inventory.StackMode.STACK_ALWAYS)

    protected var npcDefinition: NpcDefinition? = null
    val size get() = npcDefinition?.size ?: 1

    protected var mobIndex: Int = -1
    val index get() = synchronized(this) { mobIndex }
    val interactionIndex get() = mobIndex
    val isActive get() = mobIndex != -1

    fun updateIndex(index: Int) {
        synchronized(this) {
            this.mobIndex = index
        }
    }

    var firstDirection: Direction = Direction.NONE
    var lastDirection: Direction = Direction.NORTH
    var secondDirection: Direction = Direction.NONE
    fun setDirections(first: Direction, second: Direction) {
        firstDirection = first
        secondDirection = second
    }

    var blockSet: SynchronizationBlockSet = SynchronizationBlockSet()
    fun resetBlockSet() {
        blockSet = SynchronizationBlockSet()
    }


    private var action: Action<*>? = null
    fun startAction(action: Action<*>): Boolean {
        if (this.action != null) {
            if (this.action == action)
                return false

            stopAction()
        }

        this.action = action
        return world.schedule(action)
    }

    fun stopAction() {
        action?.stop()
        action = null
    }


    val hasInteractingMob get() = interactingMob != null
    protected var interactingMob: Mob? = null
        set(value) {
            field = value
            blockSet.add(InteractingMobBlock.createFrom(value?.interactionIndex ?: error("Uhm, noo...")))
        }

    fun resetInteractingMob() {
        interactingMob = null
        blockSet.add(InteractingMobBlock.createFrom(InteractingMobBlock.RESET_INDEX))
    }
}