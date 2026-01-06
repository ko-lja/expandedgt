package lu.kolja.expandedgt.definiton

import com.gregtechceu.gtceu.api.GTValues.UV
import com.gregtechceu.gtceu.api.GTValues.ZPM
import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.ExpandedGT.Companion.REGISTRATE
import lu.kolja.expandedgt.lang.ExpTooltips
import lu.kolja.expandedgt.util.sharedPart
import lu.kolja.expandedgt.xmod.*
import lu.kolja.expandedgt.xmod.dual.DualMEOutputHatchPartMachine

object ExpandedGTMachines {
    init {
        REGISTRATE.creativeModeTab(ExpandedGT::CREATIVE_TAB)
    }

    val ExpandedMEInputHatch: MachineDefinition = REGISTRATE
        .machine("expanded_me_input_hatch") { holder -> ExpMEInputHatchPartMachine(holder) }
        .langValue("Expanded ME Input Hatch")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_input_hatch"))
        .tooltips(ExpTooltips.EvenBigger.text("ME Input Hatch", 32))
        .sharedPart()
        .register()

    val ExpandedMEInputBus: MachineDefinition = REGISTRATE
        .machine("expanded_me_input_bus", ::ExpMEInputBusPartMachine)
        .langValue("Expanded ME Input Bus")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_input_bus"))
        .tooltips(ExpTooltips.EvenBigger.text("ME Input Bus", 32))
        .sharedPart()
        .register()

    val ExpandedMEStockingHatch: MachineDefinition = REGISTRATE
        .machine("expanded_me_stocking_input_hatch", ::ExpMEStockingHatchPartMachine)
        .langValue("Expanded ME Stocking Input Hatch")
        .tier(UV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_stocking_input_hatch"))
        .tooltips(ExpTooltips.EvenBigger.text("ME Stocking Input Hatch", 32))
        .sharedPart()
        .register()

    val ExpandedMEStockingBus: MachineDefinition = REGISTRATE
        .machine("expanded_me_stocking_input_bus", ::ExpMEStockingBusPartMachine)
        .langValue("Expanded ME Stocking Input Bus")
        .tier(UV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_stocking_input_bus"))
        .tooltips(ExpTooltips.EvenBigger.text("ME Stocking Input Bus", 32))
        .sharedPart()
        .register()

    val METagFilterStockingBus: MachineDefinition = REGISTRATE
        .machine("me_tag_filter_stocking_bus", ::METagFilterStockBusPartMachine)
        .langValue("ME Tag Filter Stocking Input Bus")
        .tier(UV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/me_tag_filter_stocking_bus"))
        .tooltips(ExpTooltips.TagFilterMachineTooltip.text("ME Stocking Input Bus"))
        .sharedPart()
        .register()

    val METagFilterStockingHatch: MachineDefinition = REGISTRATE
        .machine("me_tag_filter_stocking_hatch", ::METagFilterStockHatchPartMachine)
        .langValue("ME Tag Filter Stocking Input Hatch")
        .tier(UV)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_FLUIDS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/me_tag_filter_stocking_hatch"))
        .tooltips(ExpTooltips.TagFilterMachineTooltip.text("ME Stocking Input Hatch"))
        .sharedPart()
        .register()

    val DualMEOutputHatch: MachineDefinition = REGISTRATE
        .machine("dual_me_output_hatch") {
            DualMEOutputHatchPartMachine(it, IO.OUT, ZPM)
        }.langValue("Dual ME Output Hatch")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/dual_me_output_hatch"))
        .tooltips(ExpTooltips.EvenBigger.text("Dual ME Output Hatch", 32))
        .sharedPart()
        .register()
}