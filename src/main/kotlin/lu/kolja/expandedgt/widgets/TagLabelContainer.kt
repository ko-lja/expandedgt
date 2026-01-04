package lu.kolja.expandedgt.widgets

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import lu.kolja.expandedgt.interfaces.ITagFilterPartMachine
import lu.kolja.expandedgt.lang.ExpTooltips
import net.minecraft.client.Minecraft
import java.util.concurrent.atomic.AtomicInteger

object TagLabelContainer {
    fun createTagLabelContainer(textField: ITagFilterPartMachine.TextField, tags: List<String>): Array<Widget?> {
        val atomicInt = AtomicInteger(0)
        val container = arrayOfNulls<Widget>(tags.size)
        for (tag in tags) {
            container[atomicInt.get()] = object : LabelWidget(4, atomicInt.andIncrement * 12 + 4, tag) {
                override fun mouseReleased(
                    mouseX: Double,
                    mouseY: Double,
                    button: Int
                ): Boolean {
                    if (isMouseOverElement(mouseX, mouseY)) {
                        if (button == 0) {
                            textField.setDirectly(tag)
                        } else if (button == 1) {
                            Minecraft.getInstance().keyboardHandler.clipboard = tag
                        }
                        playButtonClickSound()
                        return true
                    }
                    return super.mouseReleased(mouseX, mouseY, button)
                }
            }.setTextColor(0x39c5bb).setHoverTooltips(ExpTooltips.TagFilterInfo.text).setClientSideWidget()
        }
        return container
    }
}