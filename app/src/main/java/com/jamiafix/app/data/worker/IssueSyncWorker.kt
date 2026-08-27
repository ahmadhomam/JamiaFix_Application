package com.jamiafix.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jamiafix.app.JamiaFixApp

class IssueSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as JamiaFixApp
            val count = app.issueRepository.syncPendingIssues()
            if (count > 0) {
                // Refresh list
                app.issueRepository.refreshIssues()
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
