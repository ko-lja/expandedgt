package lu.kolja.expandedgt.mixins.gtceu;

import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import lu.kolja.expandedgt.pattern.PatternError;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MultiblockState.class, remap = false)
public abstract class MixinMultiblockState {
    private PatternError error;

    private final List<PatternError> errors = new ArrayList<>();
}
