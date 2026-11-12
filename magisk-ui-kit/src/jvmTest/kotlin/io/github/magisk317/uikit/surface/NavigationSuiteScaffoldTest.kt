package io.github.magisk317.uikit.surface

import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NavigationSuiteScaffoldTest {
    @Test
    fun widthClassesRespectMaterialBreakpoints() {
        assertEquals(NavigationWidthClass.Compact, navigationWidthClass(599.dp))
        assertEquals(NavigationWidthClass.Medium, navigationWidthClass(600.dp))
        assertEquals(NavigationWidthClass.Medium, navigationWidthClass(839.dp))
        assertEquals(NavigationWidthClass.Expanded, navigationWidthClass(840.dp))
    }
}
