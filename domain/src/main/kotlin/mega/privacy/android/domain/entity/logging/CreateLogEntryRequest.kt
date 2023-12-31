package mega.privacy.android.domain.entity.logging

/**
 * Create log entry request
 *
 * @property tag
 * @property message
 * @property priority
 * @property throwable
 * @property trace
 * @property loggingClasses
 * @property sdkLoggers
 */
data class CreateLogEntryRequest(
    val tag: String?,
    val message: String,
    val priority: LogPriority,
    val throwable: Throwable?,
    val trace: List<StackTraceElement>,
    val loggingClasses: List<String>,
    val sdkLoggers: List<String>,
)