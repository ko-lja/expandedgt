package lu.kolja.expandedgt.common.machines.input.stocking

import appeng.api.config.Actionable
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingFancyConfigurator
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.config.ConfigHolder
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.Object2LongMap
import lombok.Getter
import lombok.Setter
import lu.kolja.expandedgt.common.machines.input.ExpMEInputHatchPartMachine
import lu.kolja.expandedgt.util.translate
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import java.util.*
import java.util.function.Predicate


class ExpMEStockingHatchPartMachine(holder: IMachineBlockEntity, vararg args: Any): ExpMEInputHatchPartMachine(holder, args), IMEStockingPart {
    override val managedFieldHolder = ManagedFieldHolder(ExpMEStockingHatchPartMachine::class.java, super.managedFieldHolder)

    @DescSynced
    @Persisted
    @Getter
    private var autoPull: Boolean = false

    @Persisted
    @DropSaved
    private var minStackSize: Int = 1
    @Persisted
    @DropSaved
    private var stockingTicksPerCycle: Int = 40

    @Setter
    private var autoPullTest: Predicate<GenericStack>? = null

    init {
        this.autoPullTest = Predicate { false }
    }

    override fun addedToController(controller: IMultiController) {
        super<ExpMEInputHatchPartMachine>.addedToController(controller)
        super<IMEStockingPart>.addedToController(controller)
    }

    override fun removedFromController(controller: IMultiController) {
        super<ExpMEInputHatchPartMachine>.removedFromController(controller)
        super<IMEStockingPart>.removedFromController(controller)
    }

    override fun createTank(
        initialCapacity: Int,
        slots: Int,
        vararg args: Any
    ): NotifiableFluidTank {
        this.aeFluidHandler = ExportOnlyAEStockingFluidList(this, SIZE)
        return this.aeFluidHandler
    }

    override fun getFieldHolder() = managedFieldHolder

    override fun autoIO() {
        super.autoIO()
        if (getTicksPerCycle() == 0) setTicksPerCycle(ConfigHolder.INSTANCE.compat.ae2.updateIntervals)
        if (offsetTimer.toInt() % getTicksPerCycle() == 0) {
            if (autoPull) refreshList()
            syncME()
        }
    }

    override fun syncME() {
        this.mainNode.grid?.let {
            val networkInv = it.storageService.inventory
            for (slot in this.aeFluidHandler.inventory) {
                val config = slot.config
                if (config != null) {
                    val key = config.what
                    val extracted = networkInv.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, actionSource)
                    if (extracted >= minStackSize) {
                        slot.stock = GenericStack(key, extracted)
                        continue
                    }
                }
                slot.stock = null
            }
        }
    }

    override fun attachSideTabs(sideTabs: TabsWidget) {
        sideTabs.mainTab = this
    }

    override fun flushInventory() {
        // NO-OP
    }

    override fun getSlotList(): IConfigurableSlotList = this.aeFluidHandler

    override fun testConfiguredInOtherPart(config: GenericStack?): Boolean {
        if (config == null) return false
        if (!isFormed) return false

        for (controller in controllers) {
            for (part in controller.parts) {
                if (part is ExpMEStockingHatchPartMachine) {
                    if (part == this) continue
                    if (part.aeFluidHandler.hasStackInConfig(config, false)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun getMinStackSize() = this.minStackSize

    override fun setMinStackSize(newSize: Int) {
        this.minStackSize = newSize
    }

    override fun getTicksPerCycle(): Int = stockingTicksPerCycle

    override fun setTicksPerCycle(newSize: Int) {
        stockingTicksPerCycle = newSize
    }

    override fun isAutoPull() = autoPull

    override fun setAutoPull(autoPull: Boolean) {
        this.autoPull = autoPull
        if (!isRemote) {
            if (!this.autoPull) this.aeFluidHandler.clearInventory(0)
            else if (updateMEStatus()) {
                this.refreshList()
                updateTankSubscription()
            }
        }
    }

    override fun setAutoPullTest(test: Predicate<GenericStack>?) {
        this.autoPullTest = test
    }

    fun refreshList() {
        val grid = this.mainNode.grid
        if (grid == null) {
            this.aeFluidHandler.clearInventory(0)
            return
        }

        val networkStorage = grid.storageService.inventory
        val counter = networkStorage.availableStacks

        val topItems = PriorityQueue<Object2LongMap.Entry<AEKey>>(
            compareBy { it.longValue }
        )

        for (entry in counter) {
            val amount = entry.longValue
            val what = entry.key
            if (amount <= 0) continue
            if (what !is AEItemKey) continue

            val request = networkStorage.extract(what, amount, Actionable.SIMULATE, this.actionSource)
            if (request == 0L) continue

            if (autoPullTest != null && !autoPullTest!!.test(GenericStack(what, amount))) continue
            if (amount >= minStackSize) {
                if (topItems.size < SIZE) {
                    topItems.offer(entry)
                } else if (amount > topItems.peek().longValue) {
                    topItems.poll()
                    topItems.offer(entry)
                }
            }
        }

        var index = 0
        val itemAmount = topItems.size
        for (i in 0..<SIZE) {
            index++
            if (topItems.isEmpty()) break
            val entry = topItems.poll()

            val what = entry.key
            val amount = entry.longValue

            val request = networkStorage.extract(what, amount, Actionable.SIMULATE, this.actionSource)

            val slot = this.aeFluidHandler.inventory[itemAmount - i - 1]
            slot.config = GenericStack(what, 1)
            slot.stock = GenericStack(what, request)
        }
        aeFluidHandler.clearInventory(index)
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super<IMEStockingPart>.attachConfigurators(configuratorPanel)
        super<ExpMEInputHatchPartMachine>.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(AutoStockingFancyConfigurator(this))
    }

    override fun onScrewdriverClick(
        playerIn: Player,
        hand: InteractionHand,
        gridSide: Direction,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!isRemote) {
            setAutoPull(!autoPull)
            if (autoPull) playerIn.sendSystemMessage("gtceu.machine.me.stocking_auto_pull_enabled".translate())
            else playerIn.sendSystemMessage("gtceu.machine.me.stocking_auto_pull_disabled".translate())
        }
        return InteractionResult.sidedSuccess(isRemote)
    }

    override fun writeConfigToTag(): CompoundTag {
        if (!autoPull) {
            val tag = super.writeConfigToTag()
            tag.putBoolean("AutoPull", false)
            return tag;
        }
        // if in auto-pull, no need to write actual configured slots, but still need to write the ghost circuit
        val tag = CompoundTag();
        tag.putBoolean("AutoPull", true)
        tag.putByte("GhostCircuit", IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0)).toByte())
        return tag
    }

    override fun readConfigFromTag(tag: CompoundTag) {
        if (tag.getBoolean("AutoPull")) {
            setAutoPull(true)
            circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit").toInt()))
            return
        }
        setAutoPull(false)
        super.readConfigFromTag(tag)
    }

    private inner class ExportOnlyAEStockingFluidList (holder: MetaMachine, slots: Int): ExportOnlyAEFluidList(holder, slots, ::ExportOnlyAEStockingFluidSlot) {
        override fun isAutoPull() = autoPull

        override fun isStocking() = true

        override fun hasStackInConfig(stack: GenericStack?, checkExternal: Boolean): Boolean {
            val inThisHatch = super.hasStackInConfig(stack, false)
            if (inThisHatch) return true
            if (checkExternal) return testConfiguredInOtherPart(stack)
            return false
        }
    }

    private inner class ExportOnlyAEStockingFluidSlot: ExportOnlyAEFluidSlot  {
        constructor() : super()

        constructor(config: GenericStack?, stock: GenericStack?) : super(config, stock)

        override fun drain(
            maxDrain: Int,
            action: IFluidHandler.FluidAction
        ): FluidStack {
            if (this.stock != null && this.config != null) {
                if (!isOnline()) return FluidStack.EMPTY
                mainNode.grid?.let {
                    val aeNetwork = it.storageService.inventory

                    val key = config!!.what
                    val extracted = aeNetwork.extract(key, maxDrain.toLong(), Actionable.of(action), actionSource)

                    if (extracted > 0) {
                        val resultStack = if (key is AEFluidKey) AEUtil.toFluidStack(key, extracted) else FluidStack.EMPTY
                        if (action.execute()) {
                            this.stock = copy(this.stock, stock!!.amount - extracted)
                            if (this.stock!!.amount == 0L) {
                                this.stock = null
                            }
                            if (this.onContentsChanged != null) {
                                this.onContentsChanged.run()
                            }
                        }
                        return resultStack
                    }
                }
            }
            return FluidStack.EMPTY
        }

        override fun copy(): ExportOnlyAEFluidSlot {
            return ExportOnlyAEStockingFluidSlot(
                if (this.config == null) null else copy(this.config),
                if (this.stock == null) null else copy(this.stock)
            )
        }
    }
}
