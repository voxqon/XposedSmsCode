package io.github.magisk317.smscode.verification

/** Matching for destructive inbox operations must preserve the full message identity. */
object SmsMutationPolicy {
    private val phoneAddress = Regex("""\+?[0-9() .-]+""")

    fun matchesAddress(current: String?, target: String?): Boolean {
        if (current.isNullOrBlank() || target.isNullOrBlank()) return false
        if (current == target) return true
        if (!phoneAddress.matches(current) || !phoneAddress.matches(target)) return false
        val currentDigits = current.filter { it in '0'..'9' }
        val targetDigits = target.filter { it in '0'..'9' }
        return currentDigits.isNotEmpty() && currentDigits == targetDigits
    }

    fun matchesBody(current: String?, target: String?): Boolean {
        if (current.isNullOrBlank() || target.isNullOrBlank()) return false
        return current.replace("\r\n", "\n") == target.replace("\r\n", "\n")
    }

    fun matchesTimestamp(receivedAt: Long, sentAt: Long, target: Long): Boolean =
        target > 0L && (sentAt == target || (sentAt == 0L && receivedAt == target))
}
