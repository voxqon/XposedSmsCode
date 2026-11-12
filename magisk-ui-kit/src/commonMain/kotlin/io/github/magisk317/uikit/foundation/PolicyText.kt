package io.github.magisk317.uikit.foundation

/**
 * Converts the lightweight Markdown used by the bundled privacy-policy raw
 * resources into plain text suitable for a single [androidx.compose.material3.Text].
 *
 * The rules mirror what the apps historically inlined:
 * - drop horizontal rules (`---`)
 * - drop standalone link lines (`[label](url)`)
 * - strip heading markers (`#`, `##`, `###`)
 * - strip bold markers (`**`)
 * - normalise list bullets (`*   ` / `* `) to `- `
 *
 * Pure `String -> String`; no Android or resource dependency, so it is unit
 * testable in isolation.
 */
fun stripMarkdown(raw: String): String =
    raw.lineSequence()
        .mapNotNull { line ->
            val trimmed = line.trimEnd()
            if (trimmed == "---") {
                return@mapNotNull null
            }
            if (trimmed.startsWith("[") && trimmed.contains("](")) {
                return@mapNotNull null
            }
            val noHeading = trimmed
                .removePrefix("### ")
                .removePrefix("## ")
                .removePrefix("# ")
            val noBold = noHeading.replace("**", "")
            when {
                noBold.startsWith("*   ") -> "- " + noBold.removePrefix("*   ")
                noBold.startsWith("* ") -> "- " + noBold.removePrefix("* ")
                else -> noBold
            }
        }
        .joinToString("\n")
        .trim()
