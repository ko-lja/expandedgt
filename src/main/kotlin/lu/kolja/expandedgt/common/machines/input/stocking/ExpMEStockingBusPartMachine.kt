package lu.kolja.expandedgt.common.machines.input.stocking

import appeng.api.config.Actionable
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.AutoStockingFancyConfigurator
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.config.ConfigHolder
import com.gregtechceu.gtceu.integration.ae2.machine.feature.multiblock.IMEStockingPart
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlotList
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import it.unimi.dsi.fastutil.objects.Object2LongMap
import lombok.Getter
import lombok.Setter
import lu.kolja.expandedgt.common.machines.input.ExpMEInputBusPartMachine
import lu.kolja.expandedgt.util.translate
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import java.util.*
import java.util.function.Predicate


class ExpMEStockingBusPartMachine(holder: IMachineBlockEntity, vararg args: Any): ExpMEInputBusPartMachine(holder, args), IMEStockingPart {
    override val managedFieldHolder = ManagedFieldHolder(ExpMEStockingBusPartMachine::class.java, super.managedFieldHolder)

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
        super<ExpMEInputBusPartMachine>.addedToController(controller)
        super<IMEStockingPart>.addedToController(controller)
    }

    override fun removedFromController(controller: IMultiController) {
        super<ExpMEInputBusPartMachine>.removedFromController(controller)
        super<IMEStockingPart>.removedFromController(controller)
    }

    override fun createInventory(vararg args: Any?): NotifiableItemStackHandler {
        this.aeItemHandler = ExportOnlyAEStockingItemList(this, SIZE)
        return this.aeItemHandler
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
        }
    }

    override fun attachSideTabs(sideTabs: TabsWidget) {
        sideTabs.mainTab = this
    }

    override fun flushInventory() {
        // NO-OP
    }

    override fun setDistinct(distinct: Boolean) {
        super.setDistinct(distinct)
        if (!isRemote && !isDistinct) validateConfig()
    }

    override fun getSlotList(): IConfigurableSlotList = this.aeItemHandler

    override fun testConfiguredInOtherPart(config: GenericStack?): Boolean {
        if (config == null) return false
        // In distinct mode, we don't need to check other buses since only one bus can run a recipe at a time.
        if (!isFormed || isDistinct) return false

        // Otherwise, we need to test for if the item is configured
        // in any stocking bus in the multi (besides ourselves).
        for (controller in controllers) {
            for (part in controller.parts) {
                if (part is ExpMEStockingBusPartMachine) {
                    // We don't need to check for this ourselves, as this case is handled elsewhere.
                    if (part == this || part.isDistinct) continue
                    if (part.aeItemHandler.hasStackInConfig(config, false)) {
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
            if (!this.autoPull) this.aeItemHandler.clearInventory(0)
            else if (updateMEStatus()) {
                this.refreshList()
                updateInventorySubscription()
            }
        }
    }

    override fun setAutoPullTest(test: Predicate<GenericStack>?) {
        this.autoPullTest = test
    }

    fun refreshList() {
        val grid = this.mainNode.grid
        if (grid == null) {
            this.aeItemHandler.clearInventory(0)
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

            val slot = this.aeItemHandler.inventory[itemAmount - i - 1]
            slot.config = GenericStack(what, 1)
            slot.stock = GenericStack(what, request)
        }
        aeItemHandler.clearInventory(index)
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super<IMEStockingPart>.attachConfigurators(configuratorPanel)
        super<ExpMEInputBusPartMachine>.attachConfigurators(configuratorPanel)
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


    private inner class ExportOnlyAEStockingItemList(holder: MetaMachine, slots: Int) : ExportOnlyAEItemList(holder, slots, ::ExportOnlyAEStockingItemSlot) {
        override fun isAutoPull() = autoPull

        override fun isStocking() = true

        override fun hasStackInConfig(stack: GenericStack?, checkExternal: Boolean): Boolean {
            val inThisBus = super.hasStackInConfig(stack, false)
            if (inThisBus) return true
            if (checkExternal) return testConfiguredInOtherPart(stack)
            return false
        }
    }

    private inner class ExportOnlyAEStockingItemSlot : ExportOnlyAEItemSlot {
        constructor() : super()

        constructor(config: GenericStack?, stock: GenericStack?) : super(config, stock)

        override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
            if (slot == 0 && this.stock != null) {
                if (this.config != null) {
                    // Extract the items from the real net to either validate (simulate)
                    // or extract (modulate) when this is called
                    if (!isOnline()) return ItemStack.EMPTY
                    mainNode.grid?.let {
                        val aeNetwork = it.storageService.inventory

                        val action = if (simulate) Actionable.SIMULATE else Actionable.MODULATE
                        val key = config!!.what
                        val extracted = aeNetwork.extract(key, amount.toLong(), action, actionSource)

                        if (extracted > 0) {
                            val resultStack = if (key is AEItemKey) key.toStack(extracted.toInt()) else ItemStack.EMPTY
                            if (!simulate) {
                                // may as well update the display here
                                this.stock = copy(stock, stock!!.amount - extracted)
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
}
