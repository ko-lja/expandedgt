package lu.kolja.expandedgt.mixins.ldlib;

import com.lowdragmc.lowdraglib.gui.widget.HsbColorWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;

@Mixin(value = HsbColorWidget.class, remap = false)
abstract class MixinHsbColorWidget extends Widget {
    public MixinHsbColorWidget(Position selfPosition, Size size) {
        super(selfPosition, size);
    }

    /**
     * Do not render the debug information to the player
     */
    @Inject(
            method = "renderInfo",
            at = @At("HEAD"),
            cancellable = true
    )
    private void exp$renderInfo(GuiGraphics graphics, int x, int y, int width, int height, CallbackInfo ci) {
        if (this.gui.entityPlayer != null) {
            ci.cancel();
        }
    }
}
