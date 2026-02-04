package lu.kolja.expandedgt.xmod.dual

import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.security.IActionSource
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gregtechceu.gtceu.common.machine.multiblock.part.DualHatchPartMachine
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine
import com.gregtechceu.gtceu.integration.ae2.machine.trait.GridNodeHolder
import com.lowdragmc.lowdraglib.syncdata.ISubscription
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import net.minecraft.core.Direction
import thedarkcolour.kotlinforforge.kotlin.enumSetOf
import kotlin.math.sqrt

abstract class DualMEHatchPartMachine(holder: IMachineBlockEntity, io: IO, tier: Int): DualHatchPartMachine(holder, tier, io), IGridConnectedMachine {
    companion object {
        const val INITIAL_TANK_CAPACITY = 16000

        fun getTankCapacity(initialCapacity: Int, tier: Int): Int {
            return initialCapacity * (1 shl tier - 6)
        }
    }


    val nodeHolder: GridNodeHolder

    @DescSynced
    var isNodeOnline = false

    val actionSource: IActionSource

    open val managedFieldHolder = ManagedFieldHolder(DualMEHatchPartMachine::class.java, MANAGED_FIELD_HOLDER)

    @Persisted
    val fluidTank: NotifiableFluidTank
    var iTankSubscription: ISubscription? = null

    init {
        this.fluidTank = createTank(INITIAL_TANK_CAPACITY, sqrt(this.inventorySize.toDouble()).toInt())
        nodeHolder = createNodeHolder()
        this.actionSource = IActionSource.ofMachine(nodeHolder.getMainNode()::getNode)
    }

    fun createTank(initialCapacity: Int, slots: Int): NotifiableFluidTank {
        return NotifiableFluidTank(this, slots, getTankCapacity(initialCapacity, this.getTier()), this.io)
    }

    override fun onLoad() {
        super.onLoad()
        this.iTankSubscription = this.fluidTank.addChangedListener(this::updateInventorySubscription)
    }

    override fun onUnload() {
        super.onUnload()
        iTankSubscription?.let {
            it.unsubscribe()
            iTankSubscription = null
        }
    }



    override fun updateInventorySubscription() {
        if (shouldSubscribe()) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO)
        } else if (autoIOSubs != null) {
            this.autoIOSubs!!.unsubscribe()
            autoIOSubs = null
        }
    }

    override fun onRotated(oldFacing: Direction, newFacing: Direction) {
        super.onRotated(oldFacing, newFacing)
        this.mainNode.setExposedOnSides(enumSetOf(newFacing))
    }

    open fun shouldSubscribe() = this.isWorkingEnabled && this.isNodeOnline

    override fun swapIO() = false

    override fun getFieldHolder() = managedFieldHolder

    override fun isOnline() = isNodeOnline

    override fun setOnline(isOnline: Boolean) {
        this.isNodeOnline = isOnline
    }

    override fun getMainNode(): IManagedGridNode = this.nodeHolder.getMainNode()

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State?) {
        super.onMainNodeStateChanged(reason)
        this.updateInventorySubscription()
    }

    fun createNodeHolder(): GridNodeHolder {
        return GridNodeHolder(this)
    }
}