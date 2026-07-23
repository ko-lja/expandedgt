package lu.kolja.expandedgt.menu

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory
import com.lowdragmc.lowdraglib.gui.modular.ModularUI
import com.lowdragmc.lowdraglib.gui.widget.*
import com.lowdragmc.lowdraglib.gui.widget.layout.Align
import com.lowdragmc.lowdraglib.utils.Size
import lu.kolja.expandedgt.widgets.SimpleToggleButtonWidget
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
/**
* A menu for the [lu.kolja.expandedgt.items.linked.LinkedTerminalItem]
*/
class LinkedTerminalMenu: IItemUIFactory {
    override fun createUI(
        holder: HeldItemUIFactory.HeldItemHolder,
        player: Player
    ): ModularUI {
        val ui = ModularUI(Size(180, 180), holder, player)
        ui.widget(createWidget(holder.held))
        return ui
    }

    fun createWidget(held: ItemStack): Widget {
        val useHatches = Setting(USE_HATCHES_TAG, false)
        val group = WidgetGroup(0, 0, 180, 180)
        group.addWidget(
            DraggableScrollableWidgetGroup(10, 10, 160, 160)
                .setBackground(GuiTextures.DISPLAY)
                .setYScrollBarWidth(3)
                .setYBarStyle(null, ColorPattern.BLACK.rectTexture().setRadius(1f))
                .addWidget(LabelWidget(8, 5, "Use Hatches"))
                .addWidget(
                    SimpleToggleButtonWidget(
                        142, 5, 10, 10,
                        { useHatches.getValue(held) },
                        { useHatches.setValue(held, it) })
                )
                .setAlign(Align.CENTER)
        )
        return group
    }

    companion object {
        const val USE_HATCHES_TAG = "useHatches"
    }

    data class Setting(val id: String, val defaultValue: Boolean) {
        fun setValue(stack: ItemStack, value: Boolean) {
            stack.orCreateTag.putBoolean(id, value)
        }

        fun getValue(stack: ItemStack): Boolean {
            return stack.tag?.getBoolean(id) ?: defaultValue
        }
    }
}
