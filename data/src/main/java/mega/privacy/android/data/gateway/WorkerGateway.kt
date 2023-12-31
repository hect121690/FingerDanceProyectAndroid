package mega.privacy.android.data.gateway

/**
 * Worker Gateway
 */
interface WorkerGateway {

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of CameraUploadsService
     *
     */
    suspend fun fireCameraUploadJob()

    /**
     * Fire a request to stop camera upload service.
     *
     * @param aborted true if the Camera Uploads has been aborted prematurely
     */
    suspend fun fireStopCameraUploadJob(aborted: Boolean = true)

    /**
     * Schedule job of camera upload
     *
     * @return The result of schedule job
     */
    suspend fun scheduleCameraUploadJob()

    /**
     * Restart Camera Uploads by executing [StopCameraUploadWorker] and [StartCameraUploadWorker]
     * sequentially through Work Chaining
     *
     */
    suspend fun fireRestartCameraUploadJob()

    /**
     * Reschedule Camera Upload with time interval
     */
    suspend fun rescheduleCameraUpload()

    /**
     * Stop the camera upload work by tag.
     * Stop regular camera upload sync heartbeat work by tag.
     *
     */
    suspend fun stopCameraUploadSyncHeartbeatWorkers()
}
