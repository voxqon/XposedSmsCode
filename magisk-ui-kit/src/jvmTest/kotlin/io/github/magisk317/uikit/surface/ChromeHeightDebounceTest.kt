package io.github.magisk317.uikit.surface

import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 源码契约测试（ui-kit 单测无 Robolectric / 无 kotlin.test，故用源文本正则
 * 锁定 compact chrome 的高度上报去抖接线）。
 *
 * 背景：chrome 进出场为 160ms tween，动画期间 onSizeChanged 每帧回调中间态
 * 高度；透传给 onHeightChanged 会让 MainTabScaffold 的 contentBottomPadding
 * 跟着切 tab 抖。本类只做正向断言，禁反向断言（注释历史文字会误杀）。
 */
class ChromeHeightDebounceTest {

    private val chromeSource: String by lazy {
        File("src/androidMain/kotlin/io/github/magisk317/uikit/surface/ChromeOverlays.kt").readText()
    }

    @Test
    fun `debounce window tracks the chrome animation duration`() {
        assertTrue(
            chromeSource.contains("heightDebounceMillis = animationMillis.toLong()"),
            "debounce window must derive from the chrome animation duration parameter",
        )
    }

    @Test
    fun `size samples are routed into debounce state`() {
        assertTrue(
            chromeSource.contains("sampledHeight.value = size.height.toDp()"),
            "onSizeChanged must store the sample instead of forwarding it directly",
        )
    }

    @Test
    fun `emission is keyed on sample and delayed by window`() {
        val effect = Regex("LaunchedEffect\\(sampledHeight\\.value\\)").find(chromeSource)
        assertTrue(effect != null, "emission must be driven by a LaunchedEffect keyed on the sample")
        assertTrue(
            chromeSource.contains("delay(heightDebounceMillis)"),
            "the steady height must only be emitted after the debounce window",
        )
    }

    @Test
    fun `debounced steady height is forwarded via onHeightChanged`() {
        assertTrue(
            chromeSource.contains("onHeightChanged(height)"),
            "the debounced steady height must be forwarded as onHeightChanged(height)",
        )
    }

    @Test
    fun `scaffold consumer wiring stays a straight pass-through`() {
        val scaffoldSource = File(
            "src/androidMain/kotlin/io/github/magisk317/uikit/surface/MainTabScaffold.kt",
        ).readText()
        assertTrue(
            scaffoldSource.contains("onHeightChanged = { compactBottomBarHeight = it }"),
            "MainTabScaffold must keep forwarding the now-stable height unchanged",
        )
    }
}
