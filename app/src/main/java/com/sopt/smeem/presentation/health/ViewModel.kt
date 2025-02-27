package com.sopt.smeem.presentation.health

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sopt.smeem.domain.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal open class ViewModel @Inject constructor() : androidx.lifecycle.ViewModel() {

    @Inject
    lateinit var healthRepository: HealthRepository

    private val _result: MutableLiveData<HealthStatus> = MutableLiveData<HealthStatus>()
    val result: LiveData<HealthStatus>
        get() = _result

    fun connect(onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                _result.value = healthRepository.getHealth().run { HealthStatus(true) }
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }
}