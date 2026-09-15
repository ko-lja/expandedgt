package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpGuiText(val text: String) : LocalizationEnum {
    CreativeTab("Expanded GT"),
    RgbSprayTitle("RGB Spray Can"),
    RgbSprayPreview("Preview"),
    RgbSprayHex("Hex"),
    TagFilterConfig("Tag Filter Config"),
    Whitelist("Whitelist"),
    Blacklist("Blacklist"),
    // Tag Filter Config Widget Operator descriptions
    Wildcard("§b* §rIndicates a Wildcard"),
    Priority("§b() §rIndicates Priority"),
    AndOr("§b& §r= §aAND    §b| §r= §aOR"),
    XorNot("§b^ §r= §aXOR    §b! §r= §aNOT");

    override fun getTranslationKey() = "gui.expandedgt.$name"

    override fun getEnglishText() = text
}