package io.github.magisk317.xposed.logging

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Compatibility facade for existing diagnostic call sites.
 * This local build has no telemetry transport or background exporter.
 */
object MagiskOtel {
    private const val NANOS_PER_MILLI = 1_000_000L

    /** Back-off after a failed installation-id lookup; long enough to skip per-span IPC storms. */
    private const val INSTANCE_ID_RETRY_MS = 60_000L

    const val DEFAULT_ENDPOINT: String = ""

    /** Single source of truth for the anonymous-analytics opt-in preference key. */
    const val KEY_ANALYTICS_ENABLED: String = "pref_enable_analytics"

    const val ANALYTICS_ENABLED_DEFAULT: Boolean = false

    /**
     * Legacy switches cannot enable telemetry in this build.
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun effectiveAnalyticsEnabled(userEnabled: Boolean, isDebugBuild: Boolean): Boolean =
        false

    data class Config(
        val enabled: Boolean = false,
        val endpoint: String = DEFAULT_ENDPOINT,
        val serviceName: String,
        val serviceVersion: String = "unknown",
        /**
         * Build commit (short sha) stamped at build time, e.g. via BuildConfig. Empty by
         * default so existing consumers keep their current behaviour; when set, every span
         * carries a `service.commit` resource attribute so the cloud side can slice by
         * version x commit instead of version alone.
         */
        val serviceCommit: String = "",
        val projectId: String = "",
        val projectName: String = "",
        val environment: String = "device",
        /**
         * Installation id captured at configure time. Empty means "not resolvable yet": the
         * exporter then falls back to [serviceInstanceIdProvider] on every span until one read
         * succeeds, instead of freezing an empty value for the whole process lifetime.
         *
         * This matters for hook processes, where the id lives in the module's preferences behind
         * a remote provider that may not be bound yet when hooks install.
         */
        val serviceInstanceId: String = "",
        /**
         * Lazy installation-id source consulted while [serviceInstanceId] is blank (or not a
         * UUID). Consumers pass a preference/provider read here; the kit caches the first valid
         * value for the process and backs off briefly after a miss so a per-span read stays cheap.
         */
        val serviceInstanceIdProvider: (() -> String?)? = null,
        /**
         * Device dimension resource attributes (e.g. device.manufacturer /
         * device.model / os.version). Consumers fill these from android.os.Build at
         * configure time; the kit stays platform-agnostic. Empty by default so
         * existing consumers keep their current behaviour.
         */
        val deviceAttributes: Map<String, String> = emptyMap(),
        val activityTracker: InstallationActivityTracker? = null,
        val connectTimeoutMs: Int = 4_000,
        val readTimeoutMs: Int = 4_000,
        /**
         * Span names that are never exported, for high-volume low-signal heartbeats.
         * Empty by default so existing consumers keep their current behaviour.
         */
        val suppressedEventNames: Set<String> = emptySet(),
        /**
         * Values of the `result` attribute that are never exported. Use this to drop success-path
         * noise while keeping skip / error / fallback spans. Empty by default.
         */
        val suppressedResultValues: Set<String> = emptySet(),
    )

    private val configRef = AtomicReference<Config?>(null)

    /**
     * First valid installation id seen in this process for [cachedInstanceIdOwner]. Keyed by
     * identity so a reconfigured process (and each unit test) resolves against its own [Config]
     * instead of inheriting another one's value.
     */
    @Volatile
    private var cachedInstanceIdOwner: Config? = null
    @Volatile
    private var cachedInstanceId: String? = null

    /** Earliest time (epoch ms) at which a failed [Config.serviceInstanceIdProvider] may retry. */
    @Volatile
    private var instanceIdRetryAtMs: Long = 0L
    @JvmStatic
    fun configure(config: Config) {
        configRef.set(config.copy(enabled = false))
    }

    /**
     * Install config only when no process-local config exists yet.
     * Useful for hook processes that may race with later host configuration.
     */
    @JvmStatic
    fun configureIfAbsent(config: Config): Boolean {
        return configRef.compareAndSet(null, config.copy(enabled = false))
    }

    @JvmStatic
    fun isEnabled(): Boolean = false

    /**
     * Discard events without resolving identities, scheduling work or opening connections.
     */
    @JvmStatic
    @JvmOverloads
    @Suppress("UNUSED_PARAMETER")
    fun event(
        name: String,
        attributes: Map<String, String> = emptyMap(),
        statusOk: Boolean = true,
    ) = Unit

    /**
     * Noise gate evaluated before a span is queued.
     *
     * Kept pure so consumers can unit-test the suppression rules without touching the exporter.
     * Both sets are empty by default, so a consumer that configures nothing keeps every span.
     */
    internal fun isSuppressed(
        config: Config,
        name: String,
        attributes: Map<String, String>,
    ): Boolean {
        if (name in config.suppressedEventNames) {
            return true
        }
        if (config.suppressedResultValues.isEmpty()) {
            return false
        }
        val result = attributes["result"] ?: return false
        return result in config.suppressedResultValues
    }

    /**
     * Attribute hygiene gate evaluated before a span is queued.
     *
     * `reason` is an identifier: only stable tokens survive (letters, digits, `_`, `.`) up to
     * [REASON_MAX_CHARS]; anything else - raw shell stderr, exception messages, free text -
     * folds to [REASON_UNCLASSIFIED] so the cloud side keeps a bounded, aggregatable value
     * domain. Other attribute values keep free text but get control characters collapsed and
     * are truncated at [ATTRIBUTE_MAX_BYTES] on a UTF-8 boundary.
     *
     * Kept pure so consumers can unit-test the gate without touching the exporter.
     */
    internal fun sanitizeAttributes(attributes: Map<String, String>): Map<String, String> {
        if (attributes.isEmpty()) return emptyMap()
        val sanitized = LinkedHashMap<String, String>(attributes.size)
        attributes.forEach { (key, value) ->
            val safeKey = key.trim()
            if (safeKey.isNotEmpty()) {
                sanitized[safeKey] =
                    when (safeKey) {
                        ATTRIBUTE_REASON -> sanitizeReason(value)
                        ATTRIBUTE_ERROR -> sanitizeErrorText(value)
                        else -> sanitizeValue(value)
                    }
            }
        }
        return sanitized
    }

    internal fun sanitizeReason(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > REASON_MAX_CHARS) return REASON_UNCLASSIFIED
        if (!REASON_TOKEN.matches(trimmed)) return REASON_UNCLASSIFIED
        // A pure-numeric token carries no signal: it is a stray number from a raw message that
        // reached the reason slot (observed as reason=21/3/16 leaking from exception text).
        if (trimmed.all { it.isDigit() }) return REASON_UNCLASSIFIED
        return trimmed
    }

    internal fun sanitizeValue(value: String): String {
        val flattened = CONTROL_CHARS.replace(value, " ")
        return StructuredLogSanitizer.truncateUtf8(flattened, ATTRIBUTE_MAX_BYTES)
    }

    /**
     * Sanitize an error/detail attribute value: flatten control characters, drop sensitive
     * query parameters such as `ipc_token` from URIs so per-install secrets never reach the
     * cloud side, then truncate to [ATTRIBUTE_MAX_BYTES].
     */
    internal fun sanitizeErrorText(value: String): String {
        val flattened = CONTROL_CHARS.replace(value, " ")
        val redacted = SENSITIVE_QUERY_PARAM.replace(flattened, "")
        return StructuredLogSanitizer.truncateUtf8(redacted, ATTRIBUTE_MAX_BYTES)
    }

    internal fun buildPayload(
        config: Config,
        name: String,
        attributes: Map<String, String>,
        statusOk: Boolean,
        nowNanos: Long = System.currentTimeMillis() * NANOS_PER_MILLI,
    ): String {
        val start = nowNanos - NANOS_PER_MILLI
        val resourceAttrs = mutableListOf(
            stringAttr("service.name", config.serviceName),
            stringAttr("service.version", config.serviceVersion),
            stringAttr("deployment.environment.name", config.environment),
        )
        if (config.serviceCommit.isNotBlank()) {
            resourceAttrs += stringAttr("service.commit", config.serviceCommit)
        }
        if (config.projectId.isNotBlank()) {
            resourceAttrs += stringAttr("gitlab.project.id", config.projectId)
        }
        if (config.projectName.isNotBlank()) {
            resourceAttrs += stringAttr("gitlab.project.name", config.projectName)
        }
        resolveInstanceId(config, nowNanos / NANOS_PER_MILLI)?.let {
            resourceAttrs += stringAttr("service.instance.id", it)
        }
        config.deviceAttributes.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                resourceAttrs += stringAttr(key, value)
            }
        }

        val spanAttrs = mutableListOf(stringAttr("magisk.event", name))
        attributes.forEach { (key, value) ->
            if (key.isNotBlank()) {
                spanAttrs += stringAttr(key, value)
            }
        }

        val statusCode = if (statusOk) 1 else 2
        return buildString {
            append("{\"resourceSpans\":[{\"resource\":{\"attributes\":[")
            append(resourceAttrs.joinToString(","))
            append("]},\"scopeSpans\":[{\"scope\":{\"name\":\"magisk.app.otel\",\"version\":\"1\"},\"spans\":[{")
            append("\"traceId\":\"").append(randomHex(32)).append("\",")
            append("\"spanId\":\"").append(randomHex(16)).append("\",")
            append("\"name\":\"").append(jsonEscape(name)).append("\",")
            append("\"kind\":1,")
            append("\"startTimeUnixNano\":\"").append(start).append("\",")
            append("\"endTimeUnixNano\":\"").append(nowNanos).append("\",")
            append("\"attributes\":[").append(spanAttrs.joinToString(",")).append("],")
            append("\"status\":{\"code\":").append(statusCode).append("}")
            append("}]}]}]}")
        }
    }

    /**
     * Resolve the anonymous installation id for a span.
     *
     * Order: the value captured in [Config.serviceInstanceId], then the lazy provider, then
     * nothing. A miss is negative-cached for [INSTANCE_ID_RETRY_MS] so a broken provider cannot
     * turn every span into an IPC round trip, while a provider that binds late still heals the
     * process within a minute. Never throws.
     */
    internal fun resolveInstanceId(
        config: Config,
        nowMs: Long = System.currentTimeMillis(),
    ): String? {
        if (cachedInstanceIdOwner === config) {
            cachedInstanceId?.let { return it }
        } else {
            cachedInstanceIdOwner = config
            cachedInstanceId = null
            instanceIdRetryAtMs = 0L
        }
        AnonymousInstallationId.canonicalUuid(config.serviceInstanceId)?.let {
            cachedInstanceId = it
            return it
        }
        val provider = config.serviceInstanceIdProvider ?: return null
        if (nowMs < instanceIdRetryAtMs) return null
        val fromProvider =
            runCatching { provider.invoke() }
                .getOrNull()
                ?.let { AnonymousInstallationId.canonicalUuid(it) }
        if (fromProvider == null) {
            instanceIdRetryAtMs = nowMs + INSTANCE_ID_RETRY_MS
            return null
        }
        cachedInstanceId = fromProvider
        return fromProvider
    }

    private fun stringAttr(key: String, value: String): String {
        return "{\"key\":\"${jsonEscape(key)}\",\"value\":{\"stringValue\":\"${jsonEscape(value)}\"}}"
    }

    private fun jsonEscape(value: String): String {
        val out = StringBuilder(value.length + 8)
        for (ch in value) {
            when (ch) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        out.append("\\u%04x".format(ch.code))
                    } else {
                        out.append(ch)
                    }
                }
            }
        }
        return out.toString()
    }

    private fun randomHex(length: Int): String {
        val raw =
            UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "")
        return raw.take(length)
    }

    /** Attribute key carrying the failure or terminal reason identifier. */
    const val ATTRIBUTE_REASON: String = "reason"

    /** Attribute key carrying raw error or detail text. */
    const val ATTRIBUTE_ERROR: String = "error"

    /** Folded reason emitted when a raw reason value is not a stable token. */
    const val REASON_UNCLASSIFIED: String = "unclassified"

    /** Longest accepted reason token; longer values fold to [REASON_UNCLASSIFIED]. */
    const val REASON_MAX_CHARS: Int = 64

    /** UTF-8 byte cap for non-reason attribute values. */
    const val ATTRIBUTE_MAX_BYTES: Int = 256

    private val REASON_TOKEN = Regex("[A-Za-z0-9_.]+")

    /** Sensitive query parameters stripped from error text before export (e.g. IPC tokens). */
    private val SENSITIVE_QUERY_PARAM = Regex("[?&]ipc_token=[^&\\s]*&?")

    private val CONTROL_CHARS = Regex("\\p{Cntrl}")
}
