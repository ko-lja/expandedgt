package lu.kolja.expandedgt.definiton

import com.gregtechceu.gtceu.api.GTValues.ZPM
import com.gregtechceu.gtceu.api.data.RotationState
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.ExpandedGT.Companion.REGISTRATE
import lu.kolja.expandedgt.lang.ExpTooltips
import lu.kolja.expandedgt.xmod.ExpMEInputBusPartMachine
import lu.kolja.expandedgt.xmod.ExpMEInputHatchPartMachine

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
        .register()

    val ExpandedMEInputBus: MachineDefinition = REGISTRATE
        .machine("expanded_me_input_bus", ::ExpMEInputBusPartMachine)
        .langValue("Expanded ME Input Bus")
        .tier(ZPM)
        .rotationState(RotationState.ALL)
        .abilities(PartAbility.IMPORT_ITEMS)
        .colorOverlayTieredHullModel(ExpandedGT.makeId("block/overlay/ae2/expanded_me_input_bus"))
        .tooltips(ExpTooltips.EvenBigger.text("ME Input Bus", 32))
        .register()
}