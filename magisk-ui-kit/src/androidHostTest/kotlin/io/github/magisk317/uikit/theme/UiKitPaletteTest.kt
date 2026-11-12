package io.github.magisk317.uikit.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UiKitPaletteTest {
    @Test
    fun `surface blur policy degrades without changing requested preference`() {
        assertEquals(UiKitSurfaceBlurTier.Disabled, resolveSurfaceBlurTier(false, true))
        assertEquals(UiKitSurfaceBlurTier.StaticFallback, resolveSurfaceBlurTier(true, false))
        assertEquals(UiKitSurfaceBlurTier.Backdrop, resolveSurfaceBlurTier(true, true))
    }

    @Test
    fun `disabled surface blur preserves the original alpha`() {
        assertEquals(
            0.2f,
            UiKitSurfaceBlurState.Disabled.staticSurfaceAlpha(0.2f),
        )
    }

    @Test
    fun `surface alpha keeps fallback readable`() {
        assertEquals(
            0.88f,
            UiKitSurfaceBlurState(true, UiKitSurfaceBlurTier.StaticFallback, null)
                .staticSurfaceAlpha(0.2f),
        )
        assertEquals(
            0.78f,
            UiKitSurfaceBlurState(true, UiKitSurfaceBlurTier.Backdrop, null)
                .staticSurfaceAlpha(1f),
        )
    }

    @Test
    fun `invalid persisted values fall back to stable defaults`() {
        assertEquals(UiKitPaletteStyle.TonalSpot, UiKitPaletteStyle.fromValue(99))
        assertEquals(UiKitColorSpec.Spec2025, UiKitColorSpec.fromValue(99))
    }

    @Test
    fun `spec 2025 is only used by supported palette styles`() {
        assertTrue(UiKitPaletteStyle.Expressive.supportsSpec2025())
        assertFalse(UiKitPaletteStyle.Rainbow.supportsSpec2025())
        assertEquals(
            UiKitColorSpec.Spec2021,
            UiKitColorSpec.Spec2025.effectiveFor(UiKitPaletteStyle.Rainbow),
        )
        assertEquals(
            UiKitColorSpec.Spec2025,
            UiKitColorSpec.Spec2025.effectiveFor(UiKitPaletteStyle.TonalSpot),
        )
    }
}
