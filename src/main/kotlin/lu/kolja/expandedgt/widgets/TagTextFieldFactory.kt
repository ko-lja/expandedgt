package lu.kolja.expandedgt.widgets

import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.loading.FMLEnvironment

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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return createClientTextField(x, y, width, height, textSupplier, textConsumer, placeholder)
        }
        return TagTextFieldWidget(x, y, width, height, textSupplier, textConsumer, placeholder)
    }

    private fun createClientTextField(
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
