package mega.privacy.android.app.service.di

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.middlelayer.reporter.CrashReporter
import mega.privacy.android.app.middlelayer.reporter.PerformanceReporter
import mega.privacy.android.app.service.reporter.FirebaseCrashReporter
import mega.privacy.android.app.service.reporter.FirebasePerformanceReporter
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AnalyticsModule {

    @Provides
    fun provideFirebaseCrashlytics(): FirebaseCrashlytics =
        FirebaseCrashlytics.getInstance()

    @Provides
    fun provideFirebasePerformance(): FirebasePerformance =
        FirebasePerformance.getInstance()

    @Singleton
    @Provides
    fun provideCrashReporter(firebaseCrashlytics: FirebaseCrashlytics): CrashReporter =
        FirebaseCrashReporter(firebaseCrashlytics)

    @Singleton
    @Provides
    fun providePerformanceReporter(firebasePerformance: FirebasePerformance): PerformanceReporter =
        FirebasePerformanceReporter(firebasePerformance)
}