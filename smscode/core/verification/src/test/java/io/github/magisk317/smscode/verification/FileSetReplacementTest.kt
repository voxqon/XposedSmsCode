package io.github.magisk317.smscode.verification

import java.io.File
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileSetReplacementTest {
    @TempDir lateinit var directory: File
    private val names = listOf("records.db", "records.db-wal", "records.db-shm")

    @Test
    fun `a replacement removes stale sidecars and reopens the database`() {
        val target = File(directory, "target").apply { mkdirs() }
        val staging = File(directory, "staging").apply { mkdirs() }
        File(target, names[0]).writeText("old")
        File(target, names[1]).writeText("old-wal")
        File(staging, names[0]).writeText("new")
        var reopened = false
        FileSetReplacement.replace(staging, target, names, {}, { reopened = true })
        assertEquals("new", File(target, names[0]).readText())
        assertFalse(File(target, names[1]).exists())
        assertEquals(true, reopened)
    }

    @Test
    fun `a failed reopen restores the database and original sidecars`() {
        val target = File(directory, "target").apply { mkdirs() }
        val staging = File(directory, "staging").apply { mkdirs() }
        File(target, names[0]).writeText("old")
        File(target, names[1]).writeText("old-wal")
        File(staging, names[0]).writeText("bad")
        assertThrows(IOException::class.java) {
            FileSetReplacement.replace(staging, target, names, {}, { throw IOException("cannot open") })
        }
        assertEquals("old", File(target, names[0]).readText())
        assertEquals("old-wal", File(target, names[1]).readText())
    }

    @Test
    fun `failure to close the live database leaves all files untouched`() {
        val target = File(directory, "target").apply { mkdirs() }
        val staging = File(directory, "staging").apply { mkdirs() }
        File(target, names[0]).writeText("old")
        File(staging, names[0]).writeText("new")
        assertThrows(IOException::class.java) {
            FileSetReplacement.replace(staging, target, names, { throw IOException("cannot close") }, {})
        }
        assertEquals("old", File(target, names[0]).readText())
        assertEquals("new", File(staging, names[0]).readText())
    }

    @Test
    fun `failed reopening cannot leave newly generated sidecars on the original database`() {
        val target = File(directory, "target").apply { mkdirs() }
        val staging = File(directory, "staging").apply { mkdirs() }
        File(target, names[0]).writeText("old")
        File(staging, names[0]).writeText("bad")
        var attempts = 0
        assertThrows(IOException::class.java) {
            FileSetReplacement.replace(staging, target, names, {}, {
                if (attempts++ == 0) {
                    File(target, names[1]).writeText("new-wal")
                    File(target, names[2]).writeText("new-shm")
                    throw IOException("cannot open")
                }
            })
        }
        assertEquals("old", File(target, names[0]).readText())
        assertFalse(File(target, names[1]).exists())
        assertFalse(File(target, names[2]).exists())
    }

    @Test
    fun `failed close after replacement preserves originals for recovery`() {
        val target = File(directory, "target").apply { mkdirs() }
        val staging = File(directory, "staging").apply { mkdirs() }
        File(target, names[0]).writeText("old")
        File(staging, names[0]).writeText("new")
        var closes = 0
        assertThrows(IOException::class.java) {
            FileSetReplacement.replace(staging, target, names, {
                if (closes++ > 0) throw IOException("still in use")
            }, { throw IOException("cannot reopen") })
        }
        val backup = target.listFiles().orEmpty().single { it.name.startsWith(".restore-backup-") }
        assertEquals("old", File(backup, names[0]).readText())
    }
}
