package lu.kolja.expandedgt.xmod

import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEFluidConfigSlotWidget
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot

class ExpAEFluidConfigWidget(x: Int, y: Int, val list: ExportOnlyAEFluidList): ConfigWidget(x, y, list.inventory, list.isStocking) {
    override fun init() {
        this.displayList = Array<IConfigurableSlot>(config.size) { ExportOnlyAEFluidSlot() }
        this.cached = Array<IConfigurableSlot>(config.size) {
            val line = it / 16
            this.addWidget(AEFluidConfigSlotWidget((it - line * 16) * 18, line * 38, this, it))
            return@Array ExportOnlyAEFluidSlot()
        }
    }

    override fun hasStackInConfig(stack: GenericStack) = this.list.hasStackInConfig(stack, true)

    override fun isAutoPull() = this.list.isAutoPull
}