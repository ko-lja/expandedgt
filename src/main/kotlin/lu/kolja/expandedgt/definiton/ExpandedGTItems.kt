package lu.kolja.expandedgt.definiton

import com.gregtechceu.gtceu.api.item.ComponentItem
import com.gregtechceu.gtceu.common.data.GTItems.attach
import com.gregtechceu.gtceu.common.item.TooltipBehavior
import com.tterrag.registrate.util.entry.ItemEntry
import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.ExpandedGT.Companion.REGISTRATE
import lu.kolja.expandedgt.items.RGBSprayBehavior
import lu.kolja.expandedgt.items.linked.LinkedTerminalItem
import lu.kolja.expandedgt.lang.ExpTooltips
import lu.kolja.expandedgt.menu.LinkedTerminalMenu
import lu.kolja.expandedgt.util.baseModel
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity

object ExpandedGTItems {
    init {
        REGISTRATE.creativeModeTab(ExpandedGT::CREATIVE_TAB)
    }

    val LinkedTerminal: ItemEntry<LinkedTerminalItem> = REGISTRATE
        .item("linked_terminal") { LinkedTerminalItem(it.stacksTo(1).rarity(Rarity.UNCOMMON)) }
        .lang("Linked Terminal")
        .baseModel()
        .onRegister(attach(LinkedTerminalMenu()))
        .register()

    val RGBSprayCanBehavior = RGBSprayBehavior { UniversalDye.asStack() }

    val RGBSprayCan: ItemEntry<ComponentItem> = REGISTRATE
        .item("rgb_spray_can") { ComponentItem.create(it.stacksTo(1).rarity(Rarity.RARE)) }
        .lang("RGB Spray Can")
        .baseModel()
        .onRegister(attach(RGBSprayCanBehavior))
        .register()

    val UniversalDye: ItemEntry<ComponentItem> = REGISTRATE
        .item("universal_dye") { ComponentItem.create(it) }
        .lang("Universal Dye")
        .baseModel()
        .onRegister(attach(TooltipBehavior {
            it.add(ExpTooltips.UniversalDyeConsumption.text())
            it.add(ExpTooltips.UniversalDyeUsage.text(RGBSprayBehavior.DYE_REFILL))
        }))
        .register()
}
