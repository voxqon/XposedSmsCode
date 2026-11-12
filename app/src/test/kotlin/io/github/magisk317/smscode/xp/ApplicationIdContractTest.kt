package io.github.magisk317.smscode.xp

import com.github.tianma8023.xposed.smscode.BuildConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ApplicationIdContractTest {
    @Test
    fun `installed package matches hook runtime and shortcut targets`() {
        val applicationId = "com.github.voxqon.xposed.smscode"
        assertEquals(applicationId, BuildConfig.APPLICATION_ID)
        assertEquals(applicationId, io.github.magisk317.smscode.hook.BuildConfig.APPLICATION_ID)
        assertEquals(applicationId, io.github.magisk317.smscode.runtime.BuildConfig.APPLICATION_ID)

        val relativePath = "presentation/src/main/res/xml/shortcuts.xml"
        val shortcuts = sequenceOf(File(relativePath), File("../$relativePath"))
            .first { it.isFile }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val intents = factory.newDocumentBuilder().parse(shortcuts).getElementsByTagName("intent")
        assertTrue(intents.length > 0)
        for (index in 0 until intents.length) {
            val attributes = intents.item(index).attributes
            assertEquals(
                applicationId,
                attributes.getNamedItemNS("http://schemas.android.com/apk/res/android", "targetPackage").nodeValue,
            )
        }
    }
}
