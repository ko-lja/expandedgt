package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpTooltips(var text: String): LocalizationEnum {
    EvenBigger("§8Even Bigger §b%s§8, now with §a%s§8 slots"),
    TagFilterMachineTooltip("§8A better version of the §b%s§8, allowing you to also filter by tags"),
    TagFilterInfo("Left-click to add tags to the filter, right-click to copy tags to the clipboard"),
    TagFilter0("§b* §8Indicates a wildcard §b()§8 Indicates Priority"),
    TagFilter1("§b& §8= Logical §aAND §b| §8= Logical §aOR §b^ §8= Logical §aXOR");

    override fun getTranslationKey() = "gui.tooltips.expandedgt.$name"

    override fun getEnglishText() = text
}