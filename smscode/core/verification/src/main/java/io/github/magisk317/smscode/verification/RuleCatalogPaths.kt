package io.github.magisk317.smscode.verification

import java.io.File

object RuleCatalogPaths {
    private val segmentPattern = Regex("[A-Za-z0-9_][A-Za-z0-9_. -]*")

    fun requireRulePath(path: String) {
        require(path.length <= 240 && path.startsWith("rules/") && path.endsWith(".json")) {
            "Invalid rule catalog path"
        }
        require(path.split('/').all { segment ->
            segmentPattern.matches(segment) &&
                !segment.contains("..") && !segment.endsWith('.') && !segment.endsWith(' ')
        }) { "Invalid rule catalog path" }
    }

    fun resolveFile(root: File, path: String): File {
        if (path != "_meta/rules-index.json") requireRulePath(path)
        val rootPath = root.canonicalFile.toPath()
        val file = File(root, path).canonicalFile
        require(file.toPath().startsWith(rootPath) && file.toPath() != rootPath) {
            "Rule catalog path escapes cache"
        }
        return file
    }
}
