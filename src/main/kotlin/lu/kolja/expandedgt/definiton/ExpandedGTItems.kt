package lu.kolja.expandedgt.definiton

import com.gregtechceu.gtceu.common.data.GTItems.attach
import com.tterrag.registrate.util.entry.ItemEntry
import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.ExpandedGT.Companion.REGISTRATE
import lu.kolja.expandedgt.items.linked.LinkedTerminalItem
import lu.kolja.expandedgt.menu.LinkedTerminalMenu
import lu.kolja.expandedgt.util.baseModel
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
}