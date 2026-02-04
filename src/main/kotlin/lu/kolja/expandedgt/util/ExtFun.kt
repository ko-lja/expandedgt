package lu.kolja.expandedgt.util

import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder
import com.tterrag.registrate.builders.ItemBuilder
import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.lang.ExpTooltips
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Item

/**
 * Translates the given string to a component
 */
fun String.translate(): MutableComponent = Component.translatable(this)

/**
 * Translates the given string to a component
 */
fun String.literal(): Component = Component.literal(this)

/**
 * Adds a tooltip to the machine definition that indicates if it can be shared with other multiblocks
 */
fun <T : MachineDefinition> MachineBuilder<T>.sharedPart(shared: Boolean = true): MachineBuilder<T> = this.tooltips(
    "gtceu.part_sharing.${if (shared) "enabled" else "disabled"}".translate()
)

/**
 * Automatically adds a base item model to the item definition
 */
fun <T : Item, P> ItemBuilder<T, P>.baseModel(): ItemBuilder<T, P> = this.model {
        ctx, prov -> prov.generated(ctx).texture("layer0", ExpandedGT.makeId("item/$name"))
}

/**
 * Adds certain info to the tooltip that's only visible when shift is pressed
 */
fun MutableList<Component>.shiftInfo(vararg hints: Component) {
    if (KeybindUtil.isShiftDown) this.addAll(hints) else this.add(ExpTooltips.ShiftInfo.text())
}

/**
 * Makes the component bold
 */
fun MutableComponent.bold(): MutableComponent = this.withStyle(ChatFormatting.BOLD)

/**
 * Executes the given [run] function if the [T] is null
 */
fun <T> T?.ifNull(run: () -> Unit) = if (this == null) run() else Unit

/**
 * Returns a ternary value
 */
infix fun <T> Boolean.q(ret: T): Ternary<T> = Ternary(this, ret)

/**
 * Returns the value if the ternary is true, otherwise returns the [ret] value
 */
infix fun <T> Ternary<T>.c(ret: T): T = if (bool) value else ret

/**
 * Returns a ternary value
 */
data class Ternary<T>(val bool: Boolean, val value: T)

/**
 * Returns a LazyTernary value
 */
infix fun <T> Boolean.q(ret: () -> T): LazyTernary<T> = LazyTernary(this, ret)

/**
 * Returns the value if the ternary is true, otherwise returns the [ret] value
 */
infix fun <T> LazyTernary<T>.c(ret: () -> T): T = if (bool) value() else ret()

/**
 * Returns a LazyTernary value
 */
data class LazyTernary<T>(val bool: Boolean, val value: () -> T)

fun test() {
    val a: String = true q "A" c "B"
    val b: String = true q { "A" } c { "B" }
}