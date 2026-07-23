package lu.kolja.expandedgt.widgets

import net.minecraft.network.chat.Component

object TagTextFieldFactory {
    private const val CLIENT_TEXT_FIELD = "lu.kolja.expandedgt.widgets.MlTextField"

    fun create(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        textSupplier: () -> String,
        textConsumer: (String) -> Unit,
        placeholder: Component = Component.empty()
    ): TagTextFieldWidget {
        return createTextField(x, y, width, height, textSupplier, textConsumer, placeholder)
    }

    private fun createTextField(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        textSupplier: () -> String,
        textConsumer: (String) -> Unit,
        placeholder: Component
    ): TagTextFieldWidget {
        val constructor = Class.forName(CLIENT_TEXT_FIELD).constructors.first { it.parameterCount == 7 }
        return constructor.newInstance(x, y, width, height, textSupplier, textConsumer, placeholder) as TagTextFieldWidget
    }
}
