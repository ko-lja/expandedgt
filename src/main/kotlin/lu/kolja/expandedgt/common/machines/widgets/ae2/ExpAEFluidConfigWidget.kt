package lu.kolja.expandedgt.common.machines.widgets.ae2

import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AmountSetWidget
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEFluidConfigSlotWidget
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEFluidSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot
import org.apache.commons.compress.harmony.pack200.PackingUtils.config

class ExpAEFluidConfigWidget(x: Int, y: Int, val list: ExportOnlyAEFluidList, slots: Array<ExportOnlyAEFluidSlot>): ConfigWidget(x, y, slots, list.isStocking) {
    lateinit var otherWidget: ExpAEFluidConfigWidget

    override fun init() {
        this.displayList = Array<IConfigurableSlot>(config.size) { ExportOnlyAEFluidSlot() }
        this.cached = Array<IConfigurableSlot>(config.size) {
            val line = it / 8
            this.addWidget(AEFluidConfigSlotWidget((it - line * 8) * 18, line * 38, this, it))
            return@Array ExportOnlyAEFluidSlot()
        }
    }

    override fun hasStackInConfig(stack: GenericStack) = this.list.hasStackInConfig(stack, true)

    override fun isAutoPull() = this.list.isAutoPull

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val superResult = super.mouseClicked(mouseX, mouseY, button)
        if (superResult && otherWidget.amountSetWidget.isVisible) {
            otherWidget.disableAmount()
        }
        return superResult
    }
}