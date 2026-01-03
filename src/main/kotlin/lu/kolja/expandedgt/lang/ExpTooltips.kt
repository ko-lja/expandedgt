package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpTooltips(var text: String): LocalizationEnum {
    EvenBigger("§8Even Bigger §b%s§8, now with §a%s§8 slots");

    override fun getTranslationKey() = "gui.tooltips.expandedgt.$name"

    override fun getEnglishText() = text
}