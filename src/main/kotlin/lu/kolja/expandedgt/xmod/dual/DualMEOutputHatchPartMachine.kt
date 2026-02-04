package lu.kolja.expandedgt.xmod.dual

import appeng.api.config.Actionable
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEItemKey
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.api.recipe.GTRecipe
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler
import com.gregtechceu.gtceu.integration.ae2.gui.widget.list.AEListGridWidget
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage
import com.gregtechceu.gtceu.utils.GTMath
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import lu.kolja.expandedgt.xmod.dual.DualMEHatchPartMachine
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import java.util.*
import java.util.function.IntFunction

class DualMEOutputHatchPartMachine(holder: IMachineBlockEntity, tier: Int): DualMEHatchPartMachine(holder, IO.OUT, tier), IDropSaveMachine {
    override val managedFieldHolder = ManagedFieldHolder(DualMEOutputHatchPartMachine::class.java, super.managedFieldHolder)

    @Persisted
    lateinit var internalItemBuffer: KeyStorage
    @Persisted
    lateinit var internalFluidBuffer: KeyStorage

    override fun createInventory(vararg args: Any?): NotifiableItemStackHandler {
        this.internalItemBuffer = KeyStorage()
        return InaccessibleInfiniteHandler(this)
    }

    override fun saveToItem(tag: CompoundTag) {
        super.saveToItem(tag)
        tag.put("internalItemBuffer", internalItemBuffer.serializeNBT())
        tag.put("internalFluidBuffer", internalFluidBuffer.serializeNBT())
    }

    override fun loadFromItem(tag: CompoundTag) {
        super.loadFromItem(tag)
        internalItemBuffer.deserializeNBT(tag.get("internalItemBuffer") as ListTag)
        internalFluidBuffer.deserializeNBT(tag.get("internalFluidBuffer") as ListTag)
    }

    override fun onLoad() {
        super.onLoad()
    }

    override fun createTank(
        initialCapacity: Int,
        slots: Int,
        vararg args: Any?
    ): NotifiableFluidTank {
        this.internalFluidBuffer = KeyStorage()
        return InaccessibleInfiniteTank(this)
    }

    override fun onMachineRemoved() {
        val node = this.mainNode
        node.grid?.let {
            val inventory = it.storageService.inventory
            for (entry in internalItemBuffer) {
                inventory.insert(entry.key, entry.longValue, Actionable.MODULATE, this.actionSource)
            }
            for (entry in internalFluidBuffer) {
                inventory.insert(entry.key, entry.longValue, Actionable.MODULATE, this.actionSource)
            }
        }
    }

    override fun autoIO() {
        if (!this.shouldSyncME()) return
        if (this.updateMEStatus()) {
            val grid = this.mainNode.grid
            if (grid != null && !this.internalItemBuffer.isEmpty) {
                this.internalItemBuffer.insertInventory(grid.storageService.inventory, this.actionSource)
            }
            this.updateInventorySubscription()
        }
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 340, 65)
        group.addWidget(LabelWidget(5, 0) { if (this.isNodeOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline" })
        group.addWidget(LabelWidget(5, 10, "gtceu.gui.waiting_list"))
        group.addWidget(AEListGridWidget.Item(5, 20, 3, this.internalItemBuffer))
        group.addWidget(AEListGridWidget.Fluid(173, 20, 3, this.internalFluidBuffer))
        return group
    }

    override fun shouldSubscribe() = super.shouldSubscribe() && (!this.internalItemBuffer.isEmpty || !this.internalFluidBuffer.isEmpty)

    inner class InaccessibleInfiniteHandler(holder: MetaMachine):
        NotifiableItemStackHandler(holder, 1, IO.OUT, IO.NONE, IntFunction {
            Objects.requireNonNull(this@DualMEOutputHatchPartMachine)
            ItemStackHandlerDelegate()
        }) {
        init {
            internalItemBuffer.setOnContentsChanged(this::onContentsChanged)
        }

        override fun getContents(): MutableList<Any> = emptyList<Any>().toMutableList()

        override fun getTotalContentAmount() = 0.toDouble()

        override fun isEmpty() = true
    }

    inner class InaccessibleInfiniteTank(holder: MetaMachine): NotifiableFluidTank(holder, listOf(FluidStorageDelegate()), IO.OUT, IO.NONE) {
        val storage: FluidStorageDelegate

        init {
            internalFluidBuffer.setOnContentsChanged(this::onContentsChanged)
            this.storage = this.storages[0] as FluidStorageDelegate
            this.allowSameFluids = true
        }

        override fun getTanks() = 128

        override fun getContents(): List<Any?> = emptyList()

        override fun getTotalContentAmount() = 0.toDouble()

        override fun isEmpty() = true

        override fun getFluidInTank(tank: Int): FluidStack = FluidStack.EMPTY

        override fun setFluidInTank(tank: Int, fluidStack: FluidStack) {}

        override fun getTankCapacity(tank: Int) = storage.capacity

        override fun isFluidValid(tank: Int, stack: FluidStack) = true

        override fun handleRecipeInner(
            io: IO,
            recipe: GTRecipe,
            left: List<FluidIngredient>,
            simulate: Boolean
        ): List<FluidIngredient>? {
            if (io != IO.OUT) return left
            else {
                val action = if (simulate) IFluidHandler.FluidAction.SIMULATE else IFluidHandler.FluidAction.EXECUTE
                val it: MutableIterator<FluidIngredient> = left.iterator() as MutableIterator<FluidIngredient>

                while (it.hasNext()) {
                    val ingredient = it.next()
                    if (ingredient.isEmpty) it.remove()
                    else {
                        val fluids = ingredient.getStacks()
                        fluids?.let { array ->
                            if (array.size != 0 && !array[0]!!.isEmpty) {
                                val output = array[0]
                                ingredient.shrink(storage.fill(output!!, action))
                                if (ingredient.amount <= 0) it.remove()
                                continue
                            }
                        }
                        it.remove()
                    }
                }
                return left.ifEmpty { null }
            }
        }
    }

    inner class FluidStorageDelegate: CustomFluidTank(0) {
        override fun getCapacity() = Integer.MAX_VALUE

        override fun setFluid(stack: FluidStack) {}

        override fun fill(
            resource: FluidStack,
            action: IFluidHandler.FluidAction
        ): Int {
            val key = AEFluidKey.of(resource.fluid, resource.tag)
            val amount = resource.amount
            val oldValue = internalFluidBuffer.storage.getOrDefault(key, 0)
            val changeValue = (Long.MAX_VALUE - oldValue).coerceAtMost(amount.toLong())
            if (changeValue > 0 && action.execute()) {
                internalFluidBuffer.storage.put(key, oldValue + changeValue)
                internalFluidBuffer.onChanged()
            }
            return GTMath.saturatedCast(changeValue)
        }

        override fun supportsFill(tank: Int) = false

        override fun supportsDrain(tank: Int) = false
    }

    inner class ItemStackHandlerDelegate: CustomItemStackHandler() {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
        }

        override fun getSlots(): Int {
            return Short.MAX_VALUE.toInt()
        }

        override fun getSlotLimit(slot: Int): Int {
            return Integer.MAX_VALUE
        }

        override fun getStackInSlot(slot: Int): ItemStack {
            return ItemStack.EMPTY
        }

        override fun setStackInSlot(slot: Int, stack: ItemStack) {}

        override fun insertItem(
            slot: Int,
            stack: ItemStack,
            simulate: Boolean
        ): ItemStack {
            val key = AEItemKey.of(stack)
            val count = stack.count
            val oldValue = internalItemBuffer.storage.getOrDefault(key, 0)
            val changeValue = (Long.MAX_VALUE - oldValue).coerceAtMost(count.toLong())
            if (changeValue > 0) {
                if (!simulate) {
                    internalItemBuffer.storage.put(key, oldValue + changeValue)
                    internalItemBuffer.onChanged()
                }
                return stack.copyWithCount(count - changeValue.toInt())
            } else {
                return ItemStack.EMPTY
            }
        }

        override fun extractItem(
            slot: Int,
            amount: Int,
            simulate: Boolean
        ): ItemStack {
            return ItemStack.EMPTY
        }
    }

    override fun getFieldHolder() = managedFieldHolder
}