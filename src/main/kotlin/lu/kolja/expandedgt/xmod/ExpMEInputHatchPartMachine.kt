package lu.kolja.expandedgt.xmod

import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour
import com.gregtechceu.gtceu.integration.ae2.machine.MEInputHatchPartMachine
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import com.lowdragmc.lowdraglib.utils.Position
import net.minecraft.nbt.CompoundTag

class ExpMEInputHatchPartMachine(holder: IMachineBlockEntity, vararg args: Object): MEInputHatchPartMachine(holder, args) {
    val managedFieldHolder = ManagedFieldHolder(ExpMEInputHatchPartMachine::class.java, MANAGED_FIELD_HOLDER)

    companion object {
        const val SIZE = 32
    }

    override fun getFieldHolder() = managedFieldHolder

    override fun createTank(initialCapacity: Int, slots: Int, vararg args: Any): NotifiableFluidTank {
        this.aeFluidHandler = ExportOnlyAEFluidList(this, SIZE)
        return this.aeFluidHandler
    }

    override fun createUIWidget(): Widget {
        val group = WidgetGroup(Position(0, 0))
        group.addWidget(LabelWidget(3, 0,
            if (this.isOnline) "gtceu.gui.me_network.online" else "gtceu.gui.me_network.offline"
        ))
        group.addWidget(ExpAEFluidConfigWidget(3, 10, this.aeFluidHandler))
        return group
    }

    override fun writeConfigToTag(): CompoundTag {
        val tag = CompoundTag()
        val configStacks = CompoundTag()
        tag.put("ConfigStacks", configStacks)
        for (i in 0..<SIZE) {
            val slot = this.aeFluidHandler.inventory[i]
            val config = slot.config
            config?.let {
                val stackTag = GenericStack.writeTag(config)
                configStacks.put(i.toString(), stackTag)
            }
        }
        tag.putByte("GhostCircuit", IntCircuitBehaviour.getCircuitConfiguration(circuitInventory.getStackInSlot(0)).toByte())
        return tag
    }

    override fun readConfigFromTag(tag: CompoundTag) {
        if (tag.contains("ConfigStacks")) {
            val configStacks = tag.getCompound("ConfigStacks")
            for (i in 0..<SIZE) {
                val key = i.toString()
                if (configStacks.contains(key)) {
                    val configTag = configStacks.getCompound(key)
                    this.aeFluidHandler.inventory[i].config = GenericStack.readTag(configTag)
                } else {
                    this.aeFluidHandler.inventory[i].config = null
                }
            }
        }
        if (tag.contains("GhostCircuit")) {
            circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(tag.getByte("GhostCircuit").toInt()))
        }
    }
}