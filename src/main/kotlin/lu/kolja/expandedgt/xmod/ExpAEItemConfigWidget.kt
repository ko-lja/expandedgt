package lu.kolja.expandedgt.xmod

import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEItemConfigSlotWidget
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot

class ExpAEItemConfigWidget(x: Int, y: Int, val list: ExportOnlyAEItemList): ConfigWidget(x, y, list.inventory, list.isStocking) {
    override fun init() {
        this.displayList = Array<IConfigurableSlot>(config.size) { ExportOnlyAEItemSlot() }
        this.cached = Array<IConfigurableSlot>(config.size) {
            val line = it / 16
            this.addWidget(AEItemConfigSlotWidget((it - line * 16) * 18, line * 38, this, it))
            return@Array ExportOnlyAEItemSlot()
        }
    }

    override fun hasStackInConfig(stack: GenericStack) = this.list.hasStackInConfig(stack, true)

    override fun isAutoPull() = this.list.isAutoPull
}