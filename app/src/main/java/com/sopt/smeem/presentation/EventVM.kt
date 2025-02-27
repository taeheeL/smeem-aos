package com.sopt.smeem.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sopt.smeem.Smeem.Companion.AMPLITUDE
import com.sopt.smeem.event.AmplitudeEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EventVM @Inject constructor() : ViewModel() {
    fun sendEvent(event: AmplitudeEventType, properties: Map<String, Any>? = null) {
        try {
            viewModelScope.launch {
                if (properties != null) {
                    AMPLITUDE.track(event.eventName, properties)
                } else AMPLITUDE.track(event.eventName)
            }
        } catch (t: Throwable) {
            // 이벤트 발송이 기존 로직에 영향은 없도록
            Timber.tag("AMPLITUDE").e("amplitude send error!")
        }
    }
}