package io.github.magisk317.smscode.verification

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Limits apply to actual bytes read, including decompressed ZIP entries. */
object BoundedInput {
    const val MAX_BACKUP_JSON_BYTES = 16 * 1024 * 1024
    const val MAX_BACKUP_ARCHIVE_BYTES = 256L * 1024 * 1024
    const val MAX_BACKUP_ENTRIES = 128
    const val MAX_RULE_FILE_BYTES = 1024 * 1024
    const val MAX_RULE_CATALOG_BYTES = 16L * 1024 * 1024
    const val MAX_RULE_FILES = 256

    fun readBytes(input: InputStream, maxBytes: Int): ByteArray {
        require(maxBytes >= 0)
        val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
        copyTo(input, output, maxBytes.toLong())
        return output.toByteArray()
    }

    fun copyTo(input: InputStream, output: OutputStream, maxBytes: Long): Long {
        require(maxBytes >= 0)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val remaining = maxBytes - total
            val requested = if (remaining >= buffer.size) buffer.size else remaining.toInt() + 1
            val count = input.read(buffer, 0, requested)
            if (count < 0) return total
            if (count == 0) {
                val single = input.read()
                if (single < 0) return total
                if (total == maxBytes) throw IOException("Input exceeds size limit")
                output.write(single)
                total++
            } else {
                if (count.toLong() > remaining) throw IOException("Input exceeds size limit")
                output.write(buffer, 0, count)
                total += count
            }
        }
    }

    fun discard(input: InputStream, maxBytes: Long): Long =
        copyTo(input, object : OutputStream() {
            override fun write(value: Int) = Unit
            override fun write(buffer: ByteArray, offset: Int, length: Int) = Unit
        }, maxBytes)
}
