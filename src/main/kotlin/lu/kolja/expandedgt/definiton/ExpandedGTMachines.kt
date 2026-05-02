package lu.kolja.expandedgt.definiton

import com.gregtechceu.gtceu.api.GTValues.*
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.ExpandedGT.Companion.REGISTRATE
import lu.kolja.expandedgt.common.machines.dual.DualMEInputHatchPartMachine
import lu.kolja.expandedgt.common.machines.dual.DualMEOutputHatchPartMachine
import lu.kolja.expandedgt.common.machines.dual.DualMEStockingInputHatchPartMachine
import lu.kolja.expandedgt.common.machines.input.ExpMEInputBusPartMachine
import lu.kolja.expandedgt.common.machines.input.ExpMEInputHatchPartMachine
import lu.kolja.expandedgt.common.machines.input.stocking.ExpMEStockingBusPartMachine
import lu.kolja.expandedgt.common.machines.input.stocking.ExpMEStockingHatchPartMachine
import lu.kolja.expandedgt.common.machines.tag.METagFilterStockBusPartMachine
import lu.kolja.expandedgt.common.machines.tag.METagFilterStockHatchPartMachine
import lu.kolja.expandedgt.lang.ExpTooltips
import lu.kolja.expandedgt.util.sharedPart
import lu.kolja.expandedgt.util.translate

object ExpandedGTMachines {
    init {
        REGISTRATE.creativeModeTab(ExpandedGT::CREATIVE_TAB)
    }

    val ExpandedMEInputHatch: MachineDefinition = REGISTRATE
        .machine("expanded_me_input_hatch") { holder -> ExpMEInputHatchPartMachine(holder) }
        .langValue("Expanded ME Input Hatch")
        .tier(IV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_input_hatch"))
        .tooltips(ExpTooltips.EvenBigger.text("block.gtceu.me_input_hatch".translate(), 32))
        .sharedPart()
        .register()

    val ExpandedMEInputBus: MachineDefinition = REGISTRATE
        .machine("expanded_me_input_bus", ::ExpMEInputBusPartMachine)
        .langValue("Expanded ME Input Bus")
        .tier(IV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_input_bus"))
        .tooltips(ExpTooltips.EvenBigger.text("block.gtceu.me_input_bus".translate(), 32))
        .sharedPart()
        .register()

    val ExpandedMEStockingHatch: MachineDefinition = REGISTRATE
        .machine("expanded_me_stocking_input_hatch", ::ExpMEStockingHatchPartMachine)
        .langValue("Expanded ME Stocking Input Hatch")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_stocking_input_hatch"))
        .tooltips(ExpTooltips.EvenBigger.text("block.gtceu.me_stocking_input_hatch".translate(), 32))
        .sharedPart()
        .register()

    val ExpandedMEStockingBus: MachineDefinition = REGISTRATE
        .machine("expanded_me_stocking_input_bus", ::ExpMEStockingBusPartMachine)
        .langValue("Expanded ME Stocking Input Bus")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_stocking_input_bus"))
        .tooltips(ExpTooltips.EvenBigger.text("block.gtceu.me_stocking_input_bus".translate(), 32))
        .sharedPart()
        .register()

    val METagFilterStockingBus: MachineDefinition = REGISTRATE
        .machine("me_tag_filter_stocking_bus", ::METagFilterStockBusPartMachine)
        .langValue("ME Tag Filter Stocking Input Bus")
        .tier(LuV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/me_tag_filter_stocking_bus"))
        .tooltips(ExpTooltips.TagFilterMachineTooltip.text("block.gtceu.me_stocking_input_bus".translate()))
        .sharedPart()
        .register()

    val METagFilterStockingHatch: MachineDefinition = REGISTRATE
        .machine("me_tag_filter_stocking_hatch", ::METagFilterStockHatchPartMachine)
        .langValue("ME Tag Filter Stocking Input Hatch")
        .tier(LuV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/me_tag_filter_stocking_hatch"))
        .tooltips(ExpTooltips.TagFilterMachineTooltip.text("block.gtceu.me_stocking_input_hatch".translate()))
        .sharedPart()
        .register()

    val DualMEOutputHatch: MachineDefinition = REGISTRATE
        .machine("dual_me_output_hatch") {
            DualMEOutputHatchPartMachine(it, ZPM)
        }.langValue("Dual ME Output Hatch")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/dual_me_output_hatch"))
        .tooltips(ExpTooltips.DualHatch.text("block.gtceu.me_output_bus".translate(), "block.gtceu.me_output_hatch".translate()))
        .sharedPart()
        .register()

    val DualMEInputHatch: MachineDefinition = REGISTRATE
        .machine("dual_me_input_hatch") {
            DualMEInputHatchPartMachine(it, ZPM)
        }
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/dual_me_input_hatch"))
        .tooltips(ExpTooltips.DualHatch.text("block.gtceu.me_input_bus".translate(), "block.gtceu.me_input_hatch".translate()))
        .sharedPart()
        .register()

    val DualMEStockingInputHatch: MachineDefinition = REGISTRATE
        .machine("dual_me_stocking_input_hatch") {
            DualMEStockingInputHatchPartMachine(it, ZPM)
        }
        .langValue("Dual ME Stocking Input Hatch")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/dual_me_input_hatch"))
        .tooltips(ExpTooltips.DualHatch.text("block.gtceu.me_stocking_input_bus".translate(), "block.gtceu.me_stocking_input_hatch".translate()))
        .sharedPart()
        .register()
}
