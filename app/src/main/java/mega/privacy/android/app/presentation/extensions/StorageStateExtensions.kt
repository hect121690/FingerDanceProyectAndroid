package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.di.notification.getMonitorStorageStateEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase

/**
 * Extension function to get the value of state flow from [MonitorStorageStateEventUseCase]
 */
internal fun MonitorStorageStateEventUseCase.getState(): StorageState =
    invoke().value.storageState

/**
 * Helper function to get latest storage state by calling DI.
 * This method is useful for non-Android classes that Hilt does not support.
 */
internal fun getStorageState(): StorageState =
    getMonitorStorageStateEvent().getState()
