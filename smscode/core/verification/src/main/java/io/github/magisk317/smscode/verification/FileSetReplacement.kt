package io.github.magisk317.smscode.verification

import java.io.File
import java.io.IOException
import java.nio.file.Files

/** Replace a database and its sidecars together, restoring originals on an I/O or reopen failure. */
object FileSetReplacement {
    @Synchronized
    fun replace(
        stagingDirectory: File,
        targetDirectory: File,
        names: List<String>,
        beforeReplace: () -> Unit,
        afterReplace: () -> Unit,
    ) {
        require(names.isNotEmpty() && names.distinct().size == names.size)
        require(names.all { it.isNotBlank() && it != "." && it != ".." && '/' !in it && '\\' !in it && ':' !in it })
        require(File(stagingDirectory, names.first()).isFile) { "Main database is missing" }
        require(names.all { !File(targetDirectory, it).exists() || File(targetDirectory, it).isFile })
        val backup = Files.createTempDirectory(targetDirectory.toPath(), ".restore-backup-").toFile()
        val preserved = mutableListOf<String>()
        val installed = mutableListOf<String>()
        var keepBackup = false
        var installationStarted = false
        try {
            beforeReplace()
            try {
                names.forEach { name ->
                    val original = File(targetDirectory, name)
                    if (original.exists()) {
                        if (!original.renameTo(File(backup, name))) throw IOException("Cannot preserve database file")
                        preserved += name
                    }
                }
                installationStarted = true
                names.forEach { name ->
                    val staged = File(stagingDirectory, name)
                    if (staged.isFile) {
                        if (!staged.renameTo(File(targetDirectory, name))) throw IOException("Cannot install database file")
                        installed += name
                    }
                }
                afterReplace()
            } catch (failure: Exception) {
                val closeFailure = runCatching(beforeReplace).exceptionOrNull()
                if (closeFailure != null) {
                    keepBackup = true
                    failure.addSuppressed(closeFailure)
                    failure.addSuppressed(IOException("Original database preserved in ${backup.name}"))
                    throw failure
                }
                // Opening SQLite can generate sidecars even when none were present in the archive.
                (if (installationStarted) names else installed).forEach { name ->
                    val target = File(targetDirectory, name)
                    if (target.exists() && !target.delete()) {
                        keepBackup = true
                        failure.addSuppressed(IOException("Cannot clear failed database replacement"))
                    }
                }
                preserved.forEach { name ->
                    if (!File(backup, name).renameTo(File(targetDirectory, name))) {
                        keepBackup = true
                        failure.addSuppressed(IOException("Original database preserved in ${backup.name}"))
                    }
                }
                if (!keepBackup) runCatching(afterReplace).exceptionOrNull()?.let(failure::addSuppressed)
                throw failure
            }
        } finally {
            if (!keepBackup) backup.deleteRecursively()
        }
    }
}
