package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpPlayerMessages(val text: String): LocalizationEnum {
    PatternError("Expected [%s] at [%s]"),
    RgbSprayRequiresFuel("Requires Universal Dye to refill the RGB Spray Can");

    override fun getTranslationKey() = "messages.expandedgt.$name"

    override fun getEnglishText() = text
}