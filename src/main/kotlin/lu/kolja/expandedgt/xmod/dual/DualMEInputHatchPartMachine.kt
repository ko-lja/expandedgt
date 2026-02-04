package lu.kolja.expandedgt.xmod.dual

import appeng.api.config.Actionable
import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEFluidConfigWidget
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AEItemConfigWidget
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList
import com.gregtechceu.gtceu.utils.GTMath
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import com.lowdragmc.lowdraglib.utils.Position
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.fluids.capability.IFluidHandler

class DualMEInputHatchPartMachine(holder: IMachineBlockEntity, tier: Int): DualMEHatchPartMachine(holder, IO.IN, tier) {
    override val managedFieldHolder = ManagedFieldHolder(DualMEInputHatchPartMachine::class.java, super.managedFieldHolder)

    lateinit var aeItemHandler: ExportOnlyAEItemList
    lateinit var aeFluidHandler: ExportOnlyAEFluidList

    companion object {
        const val SIZE = 16
    }

    override fun createInventory(vararg args: Any?): NotifiableItemStackHandler {
        this.aeItemHandler = ExportOnlyAEItemList(this, SIZE)
        return this.aeItemHandler
    }

    override fun createTank(
        initialCapacity: Int,
        slots: Int,
        vararg args: Any?
    ): NotifiableFluidTank {
        this.aeFluidHandler = ExportOnlyAEFluidList(this, SIZE)
        return this.aeFluidHandler
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(0, 0, 318, 100)
        group.addWidget(LabelWidget(5, 0) { if (this.isNodeOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline" })
        group.addWidget(AEItemConfigWidget(5, 20, aeItemHandler))
        group.addWidget(AEFluidConfigWidget(169, 20, aeFluidHandler))
        return group
    }

    override fun saveCustomPersistedData(tag: CompoundTag, forDrop: Boolean) {
        super.saveCustomPersistedData(tag, forDrop)
        val configStacks = CompoundTag()
        tag.put("ConfigStacks", configStacks)
        for (i in 0..<SIZE) {
            val itemSlot = this.aeItemHandler.inventory[i]
            val itemConfig = itemSlot.config
            itemConfig?.let {
                val stackTag = GenericStack.writeTag(itemConfig)
                configStacks.put(i.toString(), stackTag)
            }
            val fluidSlot = this.aeFluidHandler.inventory[i]
            val fluidConfig = fluidSlot.config
            fluidConfig?.let {
                val stackTag = GenericStack.writeTag(fluidConfig)
                configStacks.put((i + SIZE).toString(), stackTag)
            }
        }
        tag.putByte("GhostCircuit", IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0)).toByte())
        tag.putBoolean("DistinctBuses", isDistinct)
    }

    override fun loadCustomPersistedData(tag: CompoundTag) {
        super.loadCustomPersistedData(tag)
        if (tag.contains("ConfigStacks")) {
            val configStacks = tag.getCompound("ConfigStacks")
            for (i in 0..<SIZE) {
                val itemKey = i.toString()
                if (configStacks.contains(itemKey)) {
                    val configTag = configStacks.getCompound(itemKey)
                    this.aeItemHandler.inventory[i].config = GenericStack.readTag(configTag)
                } else {
                    this.aeItemHandler.inventory[i].config = null
                }
                val fluidKey = (i + SIZE).toString()
                if (configStacks.contains(fluidKey)) {
                    val configTag = configStacks.getCompound(fluidKey)
                    this.aeFluidHandler.inventory[i].config = GenericStack.readTag(configTag)
                }
            }
            if (tag.contains("GhostCircuit")) {
                this.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit").toInt()))
            }
            if (tag.contains("DistinctBuses")) {
                this.setDistinct(tag.getBoolean("DistinctBuses"))
            }
        }
    }

    override fun onMachineRemoved() {
        super.onMachineRemoved()
        flushInventory()
    }

    //AE STUFF
    override fun autoIO() {
        if (!isWorkingEnabled) return
        if (!shouldSyncME()) return
        if (!updateMEStatus()) return
        this.syncME()
        this.updateInventorySubscription()
    }

    fun syncME() {
        val networkInv = this.mainNode.grid!!.storageService.inventory

        for (slot in this.aeItemHandler.inventory) {
            val exceedItem = slot.exceedStack()
            exceedItem?.let {
                val total = exceedItem.amount
                val inserted = networkInv.insert(exceedItem.what, total, Actionable.MODULATE, this.actionSource)
                if (inserted > 0) {
                    slot.extractItem(0, GTMath.saturatedCast(inserted), false)
                    continue
                }
                slot.extractItem(0, GTMath.saturatedCast(total), false)
            }
            val reqItem = slot.requestStack()
            reqItem?.let {
                val extracted = networkInv.extract(reqItem.what, reqItem.amount, Actionable.MODULATE, this.actionSource)
                if (extracted > 0L) {
                    slot.addStack(GenericStack(reqItem.what, extracted))
                }
            }
        }
        for (slot in this.aeFluidHandler.inventory) {
            val exceedFluid = slot.exceedStack()
            exceedFluid?.let {
                val total = GTMath.saturatedCast(exceedFluid.amount)
                val inserted = GTMath.saturatedCast(networkInv.insert(exceedFluid.what, exceedFluid.amount, Actionable.MODULATE, this.actionSource))
                if (inserted > 0) {
                    slot.drain(inserted, IFluidHandler.FluidAction.EXECUTE)
                    continue
                }
                slot.drain(total, IFluidHandler.FluidAction.EXECUTE)
            }
            val reqFluid = slot.requestStack()
            reqFluid?.let {
                val extracted = networkInv.extract(reqFluid.what, reqFluid.amount, Actionable.MODULATE, this.actionSource)
                if (extracted > 0L) {
                    slot.addStack(GenericStack(reqFluid.what, extracted))
                }
            }
        }
    }

    fun flushInventory() {
        val grid = this.mainNode.grid
        grid?.let {
            for (slot in this.aeItemHandler.inventory) {
                val stock = slot.stock
                stock?.let {
                    grid.storageService.inventory.insert(stock.what, stock.amount, Actionable.MODULATE, this.actionSource)
                }
            }
            for (slot in this.aeFluidHandler.inventory) {
                val stock = slot.stock
                stock?.let {
                    grid.storageService.inventory.insert(stock.what, stock.amount, Actionable.MODULATE, this.actionSource)
                }
            }
        }
    }

    override fun getFieldHolder() = managedFieldHolder
}