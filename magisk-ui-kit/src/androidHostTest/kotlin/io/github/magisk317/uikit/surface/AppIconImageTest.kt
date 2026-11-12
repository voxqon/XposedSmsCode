package io.github.magisk317.uikit.surface

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppIconImageTest {
    @Test
    fun packageIdentityIsRequiredAndPreserved() {
        assertEquals("com.example.app", AppIconLoader.resolvePackageName("com.example.app"))
        assertNull(AppIconLoader.resolvePackageName(null))
        assertNull(AppIconLoader.resolvePackageName(""))
        assertNull(AppIconLoader.resolvePackageName("   "))
    }

    @Test
    fun cacheKeyDependsOnlyOnPackageIdentityAndSize() {
        assertEquals(
            "app_icon:com.example.app:48",
            AppIconRequest(packageName = "com.example.app", sizePx = 48).cacheKey(),
        )
        assertEquals(
            AppIconRequest(packageName = "com.example.app", sizePx = 48).cacheKey(),
            AppIconRequest(packageName = "com.example.app", sizePx = 48).cacheKey(),
        )
    }
}
