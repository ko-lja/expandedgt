package lu.kolja.expandedgt.util

import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

fun String.translate(): MutableComponent = Component.translatable(this)

fun String.literal(): Component = Component.literal(this)

fun <T : MachineDefinition> MachineBuilder<T>.sharedPart(shared: Boolean = true): MachineBuilder<T> = this.tooltips(
    "gtceu.part_sharing.${if (shared) "enabled" else "disabled"}".translate()
)