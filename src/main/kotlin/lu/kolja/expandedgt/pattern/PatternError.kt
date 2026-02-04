package lu.kolja.expandedgt.pattern

import com.gregtechceu.gtceu.api.pattern.MultiblockState
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate
import lu.kolja.expandedgt.lang.ExpPlayerMessages
import lu.kolja.expandedgt.util.literal
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

open class PatternError {
    lateinit var pos: BlockPos
    lateinit var predicate: TraceabilityPredicate

    fun setWorldState(state: MultiblockState) {
        this.pos = state.pos
        this.predicate = state.predicate
    }

    open val candidates: List<List<ItemStack>>
        get() = (predicate.common + predicate.limited).map { it.getCandidates() }

    open val errorInfo: Component
        get() {
            val candidates = candidates
                .filter { it.isNotEmpty() }
                .take(6)
                .joinToString("\n") { it.first().displayName.string + if (it.size > 1) "..." else "" }
            return ExpPlayerMessages.PatternError.text(candidates.literal(), pos.toShortString())
        }
}