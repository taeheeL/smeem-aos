package com.sopt.smeem.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sopt.smeem.data.SmeemDataStore.dataStore
import com.sopt.smeem.domain.common.SmeemErrorCode
import com.sopt.smeem.domain.common.SmeemException
import com.sopt.smeem.domain.model.Authentication
import com.sopt.smeem.domain.model.LocalStatus
import com.sopt.smeem.domain.repository.LocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class LocalRepositoryImpl
@Inject
constructor(
    private val context: Context,
) : LocalRepository {
    override suspend fun setStringValue(
        key: Preferences.Key<String>,
        value: String,
    ) {
        context.dataStore.edit { storage -> storage[key] = value }
    }

    override suspend fun setBooleanValue(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ) {
        context.dataStore.edit { storage -> storage[key] = value }
    }

    override suspend fun getBooleanValue(key: String): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[booleanPreferencesKey(key)] ?: false
    }

    override fun getBooleanFlow(key: String): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: false
        }

    override suspend fun setIntValue(
        key: Preferences.Key<Int>,
        value: Int,
    ) {
        context.dataStore.edit { storage -> storage[key] = value }
    }

    override suspend fun getIntValue(key: String): Int {
        val preferences = context.dataStore.data.first()
        return preferences[intPreferencesKey(key)] ?: 0
    }

    override suspend fun remove(key: Preferences.Key<String>) {
        context.dataStore.edit { storage -> storage.remove(key) }
    }

    /**
     * LocalStorage 로 부터 Authentication 추출
     * 없을 경우, null 응답
     * (already cached on DataStore Layer)
     */
    override suspend fun getAuthentication(): Authentication? =
        context.dataStore.data
            .catch { e: Throwable ->
                Timber.tag("dataStore_auth")
                    .e("로컬스토리지(dataStore) 에 접근해 Authentication 정보를 가져오는 도중 오류가 발생했습니다.")
                emit(emptyPreferences())
            }.map { preferences: Preferences ->
                val accessToken = preferences[API_ACCESS_TOKEN]
                val refreshToken = preferences[API_REFRESH_TOKEN]

                if (accessToken != null && refreshToken != null) {
                    Authentication(
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )
                } else null
            }.first()

    override suspend fun setAuthentication(authentication: Authentication) {
        try {
            context.dataStore.edit { mutablePreferences: MutablePreferences ->
                mutablePreferences[API_ACCESS_TOKEN] =
                    requireNotNull(authentication.accessToken) { "NPE when register authentication with accessToken" }
                mutablePreferences[API_REFRESH_TOKEN] =
                    requireNotNull(authentication.refreshToken) { "NPE when register authentication with refreshToken" }
            }
        } catch (e: IOException) {
            throw SmeemException(errorCode = SmeemErrorCode.SYSTEM_ERROR)
        } catch (e: IllegalArgumentException) {
            throw SmeemException(
                errorCode = SmeemErrorCode.SYSTEM_ERROR,
                logMessage = "token 값 저장 중, null 로 접근하였습니다. (authentication = $authentication)",
            )
        }
    }

    /**
     * LocalStorage 에 accessToken 이 저장되었는지 확인
     */
    override suspend fun isAuthenticated(): Boolean =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences: Preferences -> preferences[API_ACCESS_TOKEN] }
            .firstOrNull() != null

    override suspend fun saveStatus(
        localStatus: LocalStatus,
        value: Any?,
    ) {
        try {
            when (localStatus) {
                LocalStatus.RANDOM_TOPIC_TOOL_TIP -> {
                    context.dataStore.edit { mutablePreferences: MutablePreferences ->
                        mutablePreferences[RANDOM_TOPIC_TOOL_TIP_SWITCH] = false // make it off
                    }
                }
            }
        } catch (t: Throwable) {
            throw SmeemException(errorCode = SmeemErrorCode.SYSTEM_ERROR)
        }
    }

    override suspend fun checkStatus(localStatus: LocalStatus): Boolean =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { preferences: Preferences ->
                when (localStatus) {
                    LocalStatus.RANDOM_TOPIC_TOOL_TIP ->
                        preferences[RANDOM_TOPIC_TOOL_TIP_SWITCH]
                            ?: true // 최초에는 킨 상태
                }
            }.first()

    override suspend fun clear() {
        try {
            context.dataStore.edit { preferences -> preferences.clear() }
        } catch (t: Throwable) {
            throw SmeemException(errorCode = SmeemErrorCode.SYSTEM_ERROR)
        }
    }

    companion object {
        private val API_ACCESS_TOKEN = stringPreferencesKey("api_access_token")
        private val API_REFRESH_TOKEN = stringPreferencesKey("api_refresh_token")
        private val RANDOM_TOPIC_TOOL_TIP_SWITCH =
            booleanPreferencesKey(LocalStatus.RANDOM_TOPIC_TOOL_TIP.name)
    }
}
