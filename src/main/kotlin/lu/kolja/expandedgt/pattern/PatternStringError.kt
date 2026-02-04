package lu.kolja.expandedgt.pattern

import lu.kolja.expandedgt.util.translate
import net.minecraft.network.chat.Component

data class PatternStringError(val translationKey: String): PatternError() {
    override val errorInfo: Component
        get() = translationKey.translate().append("-").append(pos.toShortString())
}