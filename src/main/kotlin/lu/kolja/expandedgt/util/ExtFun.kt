package lu.kolja.expandedgt.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

fun String.translate(): MutableComponent = Component.translatable(this)

fun String.literal(): Component = Component.literal(this)