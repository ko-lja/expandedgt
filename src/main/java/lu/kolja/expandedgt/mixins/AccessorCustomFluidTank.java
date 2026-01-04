package lu.kolja.expandedgt.mixins;

import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CustomFluidTank.class, remap = false)
public interface AccessorCustomFluidTank {
    @Invoker("onContentsChanged")
    void invokeOnContentsChanged();
}
