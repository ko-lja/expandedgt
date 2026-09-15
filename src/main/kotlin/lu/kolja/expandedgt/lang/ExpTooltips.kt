package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpTooltips(var text: String): LocalizationEnum {
    DualHatch("§8A combined version of the §b%s§8 and §b%s§8"),
    EvenBigger("§8Even Bigger §b%s§8, now with §a%s§8 slots"),
    TagFilterMachineTooltip("§8A better version of the §b%s§8, allowing you to also filter by tags"),
    TagFilterInfo("Left-click to add tags to the filter, right-click to copy tags to the clipboard"),
    ShiftInfo("§7Press §o[SHIFT] §r§7for more info"),
    BoundTo("Bound to %s"),
    LinkedTermHint("§c§oNote: This will only pull blocks from your ME network, not your inventory"),
    RgbSprayColor("§7Selected Color: §f%s"),
    RgbSprayUses("§7Uses: §b%s§7/§b%s"),
    RgbSprayFuel("§7Refuels with §dUniversal Dye"),
    RgbSprayCopyColor("§7Middle-click a painted machine to copy its color"),
    UniversalDyeConsumption("§7Consumed to refill the RGB Spray Can when needed"),
    UniversalDyeUsage("§7Refills %s uses in the RGB Spray Can per dye");

    override fun getTranslationKey() = "gui.tooltips.expandedgt.$name"

    override fun getEnglishText() = text
}