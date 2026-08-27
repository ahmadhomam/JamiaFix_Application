package com.jamiafix.app

import android.app.Application
import com.jamiafix.app.data.local.AppDatabase
import com.jamiafix.app.data.local.UserPreferences
import com.jamiafix.app.data.remote.ApiClient
import com.jamiafix.app.data.repository.AuthRepository
import com.jamiafix.app.data.repository.IssueRepository
import com.jamiafix.app.data.repository.MetadataRepository

class JamiaFixApp : Application() {

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var metadataRepository: MetadataRepository
        private set

    lateinit var issueRepository: IssueRepository
        private set

    override fun onCreate() {
        super.onCreate()

        userPreferences = UserPreferences(this)
        database = AppDatabase.getDatabase(this)

        val apiService = ApiClient.createService(userPreferences)

        authRepository = AuthRepository(apiService, userPreferences)
        metadataRepository = MetadataRepository(apiService, database.categoryDao(), database.locationDao())
        issueRepository = IssueRepository(this, apiService, database.issueDao())
    }
}
