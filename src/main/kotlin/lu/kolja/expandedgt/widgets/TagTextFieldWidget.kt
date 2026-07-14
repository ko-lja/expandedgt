package lu.kolja.expandedgt.widgets

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component

open class TagTextFieldWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    protected val textSupplier: () -> String,
    protected val textConsumer: (String) -> Unit,
    protected val placeholder: Component = Component.empty()
): WidgetGroup(x, y, width, height) {

    companion object {
        const val DEFAULT_MAX_LENGTH: Int = Int.MAX_VALUE
        const val ACTION_SET_TEXT = 1
    }

    open fun getValue(): String = textSupplier()

    open fun setDirectly(newText: String) {
        textConsumer(newText)
        sendToServer(newText)
    }

    protected fun sendToServer(newText: String) {
        writeClientAction(ACTION_SET_TEXT) { buf -> buf.writeUtf(newText) }
    }

    override fun handleClientAction(id: Int, buffer: FriendlyByteBuf) {
        if (id == ACTION_SET_TEXT) {
            textConsumer(buffer.readUtf(DEFAULT_MAX_LENGTH))
            return
        }
        super.handleClientAction(id, buffer)
    }
}
