package mega.privacy.android.app.service.reporter

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import mega.privacy.android.app.middlelayer.reporter.CrashReporter

class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics
) : CrashReporter {

    override fun report(e: Throwable) {
        crashlytics.recordException(e)
        crashlytics.sendUnsentReports()
    }

    override fun setEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
}
