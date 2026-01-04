package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpGuiText(var text: String) : LocalizationEnum {
    CreativeTab("Expanded GT"),
    TagFilterConfig("Tag Filter Config"),
    Whitelist("Whitelist"),
    Blacklist("Blacklist");

    override fun getTranslationKey() = "gui.expandedgt.$name"

    override fun getEnglishText() = text
}