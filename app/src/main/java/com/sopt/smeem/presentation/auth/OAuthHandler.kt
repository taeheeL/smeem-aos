package com.sopt.smeem.presentation.auth

import android.content.Context
import com.sopt.smeem.domain.common.SmeemException

interface OAuthHandler {
    fun isAppEnabled(context: Context): Boolean

    fun loginOnApp(
        context: Context,
        onSuccess: (String, String) -> Unit,
        onFailed: (SmeemException) -> Unit
    )

    fun loginOnWeb(
        context: Context,
        onSuccess: (String, String) -> Unit,
        onFailed: (SmeemException) -> Unit
    )
}