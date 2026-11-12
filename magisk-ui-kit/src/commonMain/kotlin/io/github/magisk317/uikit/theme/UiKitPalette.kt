package io.github.magisk317.uikit.theme

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

enum class UiKitPaletteStyle(val value: Int) {
    TonalSpot(0),
    Neutral(1),
    Vibrant(2),
    Expressive(3),
    Rainbow(4),
    FruitSalad(5),
    Monochrome(6),
    Fidelity(7),
    Content(8),
    ;

    fun toMaterialStyle(): PaletteStyle = when (this) {
        TonalSpot -> PaletteStyle.TonalSpot
        Neutral -> PaletteStyle.Neutral
        Vibrant -> PaletteStyle.Vibrant
        Expressive -> PaletteStyle.Expressive
        Rainbow -> PaletteStyle.Rainbow
        FruitSalad -> PaletteStyle.FruitSalad
        Monochrome -> PaletteStyle.Monochrome
        Fidelity -> PaletteStyle.Fidelity
        Content -> PaletteStyle.Content
    }

    companion object {
        fun fromValue(value: Int): UiKitPaletteStyle =
            entries.firstOrNull { it.value == value } ?: TonalSpot
    }
}

enum class UiKitColorSpec(val value: Int) {
    Spec2021(0),
    Spec2025(1),
    ;

    fun toMaterialSpec(): ColorSpec.SpecVersion = when (this) {
        Spec2021 -> ColorSpec.SpecVersion.SPEC_2021
        Spec2025 -> ColorSpec.SpecVersion.SPEC_2025
    }

    companion object {
        fun fromValue(value: Int): UiKitColorSpec =
            entries.firstOrNull { it.value == value } ?: Spec2025
    }
}

fun UiKitPaletteStyle.supportsSpec2025(): Boolean = when (this) {
    UiKitPaletteStyle.TonalSpot,
    UiKitPaletteStyle.Neutral,
    UiKitPaletteStyle.Vibrant,
    UiKitPaletteStyle.Expressive,
    -> true
    else -> false
}

fun UiKitColorSpec.effectiveFor(style: UiKitPaletteStyle): UiKitColorSpec =
    if (this == UiKitColorSpec.Spec2025 && !style.supportsSpec2025()) {
        UiKitColorSpec.Spec2021
    } else {
        this
    }

fun UiKitPaletteStyle.toMiuixPaletteStyle(): ThemePaletteStyle = when (this) {
    UiKitPaletteStyle.TonalSpot -> ThemePaletteStyle.TonalSpot
    UiKitPaletteStyle.Neutral -> ThemePaletteStyle.Neutral
    UiKitPaletteStyle.Vibrant -> ThemePaletteStyle.Vibrant
    UiKitPaletteStyle.Expressive -> ThemePaletteStyle.Expressive
    else -> ThemePaletteStyle.TonalSpot
}

fun UiKitColorSpec.toMiuixColorSpec(): ThemeColorSpec = when (this) {
    UiKitColorSpec.Spec2021 -> ThemeColorSpec.Spec2021
    UiKitColorSpec.Spec2025 -> ThemeColorSpec.Spec2025
}
