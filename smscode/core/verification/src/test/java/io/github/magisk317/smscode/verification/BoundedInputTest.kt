package io.github.magisk317.smscode.verification

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundedInputTest {
    @Test
    fun `exact limit and empty input are accepted`() {
        assertArrayEquals(byteArrayOf(1, 2, 3), BoundedInput.readBytes(ByteArrayInputStream(byteArrayOf(1, 2, 3)), 3))
        assertEquals(0, BoundedInput.readBytes(ByteArrayInputStream(byteArrayOf()), 0).size)
    }

    @Test
    fun `one byte over limit is rejected before output grows past limit`() {
        val output = ByteArrayOutputStream()
        assertThrows(IOException::class.java) {
            BoundedInput.copyTo(ByteArrayInputStream(ByteArray(20)), output, 10)
        }
        assertTrue(output.size() <= 10)
    }

    @Test
    fun `limit stops a stream that never ends`() {
        var bytesRead = 0
        val input = object : InputStream() {
            override fun read(): Int { bytesRead++; return 1 }
        }
        assertThrows(IOException::class.java) { BoundedInput.readBytes(input, 1024) }
        assertEquals(1025, bytesRead)
    }

    @Test
    fun `zip limit applies to decompressed bytes`() {
        val compressed = ByteArrayOutputStream()
        ZipOutputStream(compressed).use {
            it.putNextEntry(ZipEntry("backup.json"))
            it.write(ByteArray(100_000))
            it.closeEntry()
        }
        ZipInputStream(ByteArrayInputStream(compressed.toByteArray())).use {
            it.nextEntry
            assertThrows(IOException::class.java) { BoundedInput.readBytes(it, 4096) }
        }
    }

    @Test
    fun `skipped entries share the same archive budget`() {
        assertEquals(4L, BoundedInput.discard(ByteArrayInputStream(ByteArray(4)), 5))
        assertThrows(IOException::class.java) {
            BoundedInput.discard(ByteArrayInputStream(ByteArray(2)), 1)
        }
    }
}
