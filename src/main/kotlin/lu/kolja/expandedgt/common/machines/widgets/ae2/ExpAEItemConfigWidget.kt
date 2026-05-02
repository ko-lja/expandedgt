package lu.kolja.expandedgt.common.machines.widgets.ae2

import appeng.api.stacks.GenericStack
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigWidget
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEItemConfigSlotWidget
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemList
import com.gregtechceu.gtceu.integration.ae2.slot.ExportOnlyAEItemSlot
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot

open class ExpAEItemConfigWidget(x: Int, y: Int, val list: ExportOnlyAEItemList, slots: Array<ExportOnlyAEItemSlot>): ConfigWidget(x, y, slots, list.isStocking) {
    lateinit var otherWidget: ExpAEItemConfigWidget

    override fun init() {
        this.displayList = Array<IConfigurableSlot>(config.size) { ExportOnlyAEItemSlot() }
        this.cached = Array<IConfigurableSlot>(config.size) {
            val line = it / 8
            this.addWidget(AEItemConfigSlotWidget((it - line * 8) * 18, line * 38, this, it))
            return@Array ExportOnlyAEItemSlot()
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