package lu.kolja.expandedgt.common.machines.dual

import appeng.api.config.Actionable
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingFancyConfigurator
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDistinctPart
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.config.ConfigHolder
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEFluidConfigWidget
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEItemConfigWidget
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList
import com.gregtechceu.gtceu.integration.ae2.utils.AEUtil
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.Object2LongMap
import lu.kolja.expandedgt.util.translate
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import java.util.PriorityQueue
import java.util.function.Predicate

class DualMEStockingInputHatchPartMachine(holder: IMachineBlockEntity, tier: Int) :
    DualMEHatchPartMachine(holder, IO.IN, tier), IMEStockingPart {

    override val managedFieldHolder = ManagedFieldHolder(DualMEStockingInputHatchPartMachine::class.java, super.managedFieldHolder)

    lateinit var aeItemHandler: ExportOnlyAEItemList
    lateinit var aeFluidHandler: ExportOnlyAEFluidList

    @DescSynced
    @Persisted
    private var autoPull = false

    @Persisted
    @DropSaved
    private var minStackSize = 1

    @Persisted
    @DropSaved
    private var ticksPerCycle = 40

    private var autoPullTest: Predicate<GenericStack>? = Predicate { false }
    private val combinedSlotList = CombinedStockingSlotList()

    companion object {
        const val SIZE = 16
        private const val CONFIG_TAG = "DualMEStockingInputHatch"
    }

    override fun addedToController(controller: IMultiController) {
        super<DualMEHatchPartMachine>.addedToController(controller)
        super<IMEStockingPart>.addedToController(controller)
    }

    override fun removedFromController(controller: IMultiController) {
        super<DualMEHatchPartMachine>.removedFromController(controller)
        super<IMEStockingPart>.removedFromController(controller)
    }

    override fun createInventory(vararg args: Any?): NotifiableItemStackHandler {
        this.aeItemHandler = ExportOnlyAEStockingItemList(this, SIZE)
        return this.aeItemHandler
    }

    override fun createTank(
        initialCapacity: Int,
        slots: Int,
        vararg args: Any?
    ): NotifiableFluidTank {
        this.aeFluidHandler = ExportOnlyAEStockingFluidList(this, SIZE)
        return this.aeFluidHandler
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 318, 100)
        group.addWidget(LabelWidget(5, 0) { if (this.isNodeOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline" })
        group.addWidget(AEItemConfigWidget(5, 20, aeItemHandler))
        group.addWidget(AEFluidConfigWidget(169, 20, aeFluidHandler))
        return group
    }

    override fun autoIO() {
        if (!isWorkingEnabled) return
        if (!shouldSyncME()) return
        if (!updateMEStatus()) return
        if (ticksPerCycle == 0) ticksPerCycle = ConfigHolder.INSTANCE.compat.ae2.updateIntervals
        if (offsetTimer.toInt() % ticksPerCycle == 0) {
            if (autoPull) refreshList()
            syncME()
        }
        updateInventorySubscription()
    }

    private fun syncME() {
        val networkInv = mainNode.grid?.storageService?.inventory ?: return
        for (slot in this.aeItemHandler.inventory) {
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

    private fun refreshList() {
        val grid = this.mainNode.grid
        if (grid == null) {
            this.aeItemHandler.clearInventory(0)
            this.aeFluidHandler.clearInventory(0)
            return
        }

        val networkStorage = grid.storageService.inventory
        val counter = networkStorage.availableStacks

        val topItems = PriorityQueue<Object2LongMap.Entry<AEKey>>(
            compareBy { it.longValue }
        )
        val topFluids = PriorityQueue<Object2LongMap.Entry<AEKey>>(
            compareBy { it.longValue }
        )

        for (entry in counter) {
            val amount = entry.longValue
            val what = entry.key
            if (amount <= 0) continue

            val request = networkStorage.extract(what, amount, Actionable.SIMULATE, this.actionSource)
            if (request == 0L) continue
            if (autoPullTest != null && !autoPullTest!!.test(GenericStack(what, amount))) continue

            when (what) {
                is AEItemKey -> offerStockingCandidate(topItems, entry, amount)
                is AEFluidKey -> offerStockingCandidate(topFluids, entry, amount)
            }
        }

        fillAutoPullSlots(topItems, this.aeItemHandler)
        fillAutoPullSlots(topFluids, this.aeFluidHandler)
    }

    private fun offerStockingCandidate(
        queue: PriorityQueue<Object2LongMap.Entry<AEKey>>,
        entry: Object2LongMap.Entry<AEKey>,
        amount: Long
    ) {
        if (amount < minStackSize) return
        if (queue.size < SIZE) {
            queue.offer(entry)
        } else if (amount > queue.peek().longValue) {
            queue.poll()
            queue.offer(entry)
        }
    }

    private fun fillAutoPullSlots(
        queue: PriorityQueue<Object2LongMap.Entry<AEKey>>,
        handler: ExportOnlyAEItemList
    ) {
        val networkStorage = mainNode.grid?.storageService?.inventory ?: return
        var index = 0
        val itemAmount = queue.size
        while (index < SIZE && queue.isNotEmpty()) {
            val entry = queue.poll()
            val what = entry.key
            val amount = entry.longValue
            val request = networkStorage.extract(what, amount, Actionable.SIMULATE, this.actionSource)
            val slot = handler.inventory[itemAmount - index - 1]
            slot.config = GenericStack(what, 1)
            slot.stock = GenericStack(what, request)
            index++
        }
        handler.clearInventory(index)
    }

    private fun fillAutoPullSlots(
        queue: PriorityQueue<Object2LongMap.Entry<AEKey>>,
        handler: ExportOnlyAEFluidList
    ) {
        val networkStorage = mainNode.grid?.storageService?.inventory ?: return
        var index = 0
        val fluidAmount = queue.size
        while (index < SIZE && queue.isNotEmpty()) {
            val entry = queue.poll()
            val what = entry.key
            val amount = entry.longValue
            val request = networkStorage.extract(what, amount, Actionable.SIMULATE, this.actionSource)
            val slot = handler.inventory[fluidAmount - index - 1]
            slot.config = GenericStack(what, 1)
            slot.stock = GenericStack(what, request)
            index++
        }
        handler.clearInventory(index)
    }

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        tag.put(CONFIG_TAG, writeConfigToTag())
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        if (tag.contains(CONFIG_TAG)) readConfigFromTag(tag.getCompound(CONFIG_TAG))
    }

    private fun writeConfigToTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putBoolean("AutoPull", autoPull)
        tag.putInt("MinStackSize", minStackSize)
        tag.putInt("TicksPerCycle", ticksPerCycle)
        tag.putByte("GhostCircuit", IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0)).toByte())
        tag.putBoolean("DistinctBuses", isDistinct)
        if (!autoPull) {
            val configStacks = CompoundTag()
            tag.put("ConfigStacks", configStacks)
            for (i in 0..<SIZE) {
                val itemConfig = this.aeItemHandler.inventory[i].config
                if (itemConfig != null) {
                    configStacks.put(i.toString(), GenericStack.writeTag(itemConfig))
                }
                val fluidConfig = this.aeFluidHandler.inventory[i].config
                if (fluidConfig != null) {
                    configStacks.put((i + SIZE).toString(), GenericStack.writeTag(fluidConfig))
                }
            }
        }
        return tag
    }

    private fun readConfigFromTag(tag: CompoundTag) {
        if (tag.contains("MinStackSize")) {
            this.minStackSize = tag.getInt("MinStackSize").coerceAtLeast(1)
        }
        if (tag.contains("TicksPerCycle")) {
            this.ticksPerCycle = tag.getInt("TicksPerCycle")
        }
        if (tag.contains("GhostCircuit")) {
            this.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit").toInt()))
        }
        if (tag.contains("DistinctBuses")) {
            this.isDistinct = tag.getBoolean("DistinctBuses")
        }
        setAutoPull(tag.getBoolean("AutoPull"))
        if (autoPull) return

        if (tag.contains("ConfigStacks")) {
            val configStacks = tag.getCompound("ConfigStacks")
            for (i in 0..<SIZE) {
                val itemKey = i.toString()
                this.aeItemHandler.inventory[i].config = if (configStacks.contains(itemKey)) {
                    GenericStack.readTag(configStacks.getCompound(itemKey))
                } else {
                    null
                }
                val fluidKey = (i + SIZE).toString()
                this.aeFluidHandler.inventory[i].config = if (configStacks.contains(fluidKey)) {
                    GenericStack.readTag(configStacks.getCompound(fluidKey))
                } else {
                    null
                }
            }
        }
    }

    override fun setDistinct(distinct: Boolean) {
        super.setDistinct(distinct)
        if (!isRemote) validateConfig()
    }

    override fun getSlotList(): IConfigurableSlotList = combinedSlotList

    override fun testConfiguredInOtherPart(config: GenericStack?): Boolean {
        if (config == null) return false
        if (!isFormed) return false
        val isItem = config.what is AEItemKey
        if (isItem && isDistinct) return false

        for (controller in controllers) {
            for (part in controller.parts) {
                if (part == this || part !is IMEStockingPart) continue
                if (isItem && part is IDistinctPart && part.isDistinct) continue
                if (part.slotList.hasStackInConfig(config, false)) return true
            }
        }
        return false
    }

    override fun getMinStackSize() = this.minStackSize

    override fun setMinStackSize(newSize: Int) {
        this.minStackSize = newSize
    }

    override fun getTicksPerCycle() = this.ticksPerCycle

    override fun setTicksPerCycle(newSize: Int) {
        this.ticksPerCycle = newSize
    }

    override fun isAutoPull() = autoPull

    override fun setAutoPull(autoPull: Boolean) {
        this.autoPull = autoPull
        if (!isRemote) {
            if (!this.autoPull) {
                this.aeItemHandler.clearInventory(0)
                this.aeFluidHandler.clearInventory(0)
            } else if (updateMEStatus()) {
                this.refreshList()
                updateInventorySubscription()
            }
        }
    }

    override fun setAutoPullTest(test: Predicate<GenericStack>?) {
        this.autoPullTest = test
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super<IMEStockingPart>.attachConfigurators(configuratorPanel)
        super<DualMEHatchPartMachine>.attachConfigurators(configuratorPanel)
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

    override fun getFieldHolder() = managedFieldHolder

    private inner class CombinedStockingSlotList : IConfigurableSlotList {
        override fun getConfigurableSlot(index: Int): IConfigurableSlot {
            return if (index < SIZE) {
                aeItemHandler.getConfigurableSlot(index)
            } else {
                aeFluidHandler.getConfigurableSlot(index - SIZE)
            }
        }

        override fun getConfigurableSlots() = SIZE * 2

        override fun hasStackInConfig(stack: GenericStack?, checkExternal: Boolean): Boolean {
            val inThisHatch = aeItemHandler.hasStackInConfig(stack, false) || aeFluidHandler.hasStackInConfig(stack, false)
            if (inThisHatch) return true
            if (checkExternal) return testConfiguredInOtherPart(stack)
            return false
        }
    }

    private inner class ExportOnlyAEStockingItemList(holder: MetaMachine, slots: Int) :
        ExportOnlyAEItemList(holder, slots, ::ExportOnlyAEStockingItemSlot) {

        override fun isAutoPull() = autoPull

        override fun isStocking() = true

        override fun hasStackInConfig(stack: GenericStack?, checkExternal: Boolean): Boolean {
            val inThisHatch = super.hasStackInConfig(stack, false)
            if (inThisHatch) return true
            if (checkExternal) return testConfiguredInOtherPart(stack)
            return false
        }
    }

    private inner class ExportOnlyAEStockingItemSlot : ExportOnlyAEItemSlot {
        constructor() : super()

        constructor(config: GenericStack?, stock: GenericStack?) : super(config, stock)

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
            if (slot == 0 && this.stock != null && this.config != null) {
                if (!isOnline()) return ItemStack.EMPTY
                mainNode.grid?.let {
                    val aeNetwork = it.storageService.inventory
                    val action = if (simulate) Actionable.SIMULATE else Actionable.MODULATE
                    val key = config!!.what
                    val extracted = aeNetwork.extract(key, amount.toLong(), action, actionSource)
                    if (extracted > 0) {
                        val resultStack = if (key is AEItemKey) key.toStack(extracted.toInt()) else ItemStack.EMPTY
                        if (!simulate) {
                            this.stock = copy(stock, stock!!.amount - extracted)
                            if (this.stock!!.amount == 0L) {
                                this.stock = null
                            }
                            this.onContentsChanged?.run()
                        }
                        return resultStack
                    }
                }
            }
            return ItemStack.EMPTY
        }

        override fun copy(): ExportOnlyAEStockingItemSlot {
            return ExportOnlyAEStockingItemSlot(
                if (this.config == null) null else copy(this.config),
                if (this.stock == null) null else copy(this.stock)
            )
        }
    }

    private inner class ExportOnlyAEStockingFluidList(holder: MetaMachine, slots: Int) :
        ExportOnlyAEFluidList(holder, slots, ::ExportOnlyAEStockingFluidSlot) {

        override fun isAutoPull() = autoPull

        override fun isStocking() = true

        override fun hasStackInConfig(stack: GenericStack?, checkExternal: Boolean): Boolean {
            val inThisHatch = super.hasStackInConfig(stack, false)
            if (inThisHatch) return true
            if (checkExternal) return testConfiguredInOtherPart(stack)
            return false
        }
    }

    private inner class ExportOnlyAEStockingFluidSlot : ExportOnlyAEFluidSlot {
        constructor() : super()

        constructor(config: GenericStack?, stock: GenericStack?) : super(config, stock)

        override fun drain(maxDrain: Int, action: IFluidHandler.FluidAction): FluidStack {
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
                            this.onContentsChanged?.run()
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
