package lu.kolja.expandedgt.menu

import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory
import com.lowdragmc.lowdraglib.gui.modular.ModularUI
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.layout.Align
import com.lowdragmc.lowdraglib.utils.Size
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.lang.Exception

class LinkedTerminalMenu: IItemUIFactory {
    val useAE = Setting("useAE", false)

    override fun createUI(
        holder: HeldItemUIFactory.HeldItemHolder,
        player: Player
    ): ModularUI {
        val ui = ModularUI(Size(180, 180), holder, player)
        ui.widget(createWidget(player, holder.held))
        return ui
    }

    fun createWidget(player: Player, held: ItemStack): Widget {
        val hand = player.mainHandItem
        val useAe = Setting("useAE", false)
        val group = WidgetGroup(0, 0, 180, 180)
        group.addWidget(
            DraggableScrollableWidgetGroup(10, 10, 160, 160)
                .setBackground(GuiTextures.DISPLAY)
                .setYScrollBarWidth(3)
                .setYBarStyle(null, ColorPattern.BLACK.rectTexture().setRadius(1f))
                .addWidget(LabelWidget(20, 5, "Use AE2"))
                //.addWidget(CheckboxWidget(100, 5, 50, 16, { useAe.getValue(held) }, { useAe.setValue(held) }))
                //.addWidget(SwitchWidget(100, 25, 50, 16, {data, bool -> useAe.setValue(held)}))
                .setAlign(Align.CENTER)
        )
        return group
    }

    //data class Setting(val id: String)

    inner class CheckboxWidget(x: Int, y: Int, val width: Int, val height: Int, val value: () -> Boolean, val onChanged: (Boolean) -> Unit): WidgetGroup(x, y, width, height) {
        lateinit var textField: TextFieldWidget
        val min = 0
        val max = 1

        var textValue: Boolean
            set(value) = onChanged(value)
            get() = value()

        init {
            createUI()
        }

        override fun initWidget() {
            super.initWidget()
            this.textField.setCurrentString(if (value()) "1" else "0")
        }

        override fun writeInitialData(buffer: FriendlyByteBuf) {
            super.writeInitialData(buffer)
            buffer.writeBoolean(value())
        }

        override fun readInitialData(buffer: FriendlyByteBuf) {
            super.readInitialData(buffer)
            this.textField.setCurrentString(buffer.readUtf())
        }

        fun createUI() {
            textField =  object : TextFieldWidget(0, 0, width, height, { value().toString() }, { textValue = it.toInt().coerceIn(min, max) != 0 }) {
                override fun mouseWheelMove(
                    mouseX: Double,
                    mouseY: Double,
                    wheelDelta: Double
                ): Boolean {
                    if (wheelDur > 0 && numberInstance != null && isMouseOverElement(mouseX, mouseY) && isFocus) {
                        try {
                            onTextChanged("$currentString${(if (wheelDelta > 0) 1 else -1) * wheelDur}")
                        } catch (_: Exception) {}
                        isFocus = true
                        return true
                    }
                    return false
                }
            }
            textField.setNumbersOnly(min, max)
            this.addWidget(textField)
        }
    }

    data class Setting(val id: String, var value: Boolean) {
        fun setValue(stack: ItemStack) {
            value = !value
            val tag = stack.orCreateTag
            tag.putBoolean(id, value)
            stack.tag = tag
        }

        fun getValue(stack: ItemStack): Boolean {
            val tag = stack.tag
            tag?.let {
                return tag.getBoolean(id)
            }
            return value
        }
    }
}