package com.sopt.smeem.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.sopt.smeem.Smeem
import com.sopt.smeem.data.SmeemDataStore.BANNER_CLOSED
import com.sopt.smeem.data.SmeemDataStore.BANNER_VERSION
import com.sopt.smeem.domain.model.Date
import com.sopt.smeem.domain.repository.DiaryRepository
import com.sopt.smeem.domain.repository.LocalRepository
import com.sopt.smeem.domain.repository.UserRepository
import com.sopt.smeem.event.AmplitudeEventType
import com.sopt.smeem.presentation.home.calendar.core.CalendarState
import com.sopt.smeem.presentation.home.calendar.core.Period
import com.sopt.smeem.util.DateUtil
import com.sopt.smeem.util.getNextDates
import com.sopt.smeem.util.getRemainingDatesInMonth
import com.sopt.smeem.util.getRemainingDatesInWeek
import com.sopt.smeem.util.getWeekStartDate
import com.sopt.smeem.util.toYearMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val userRepository: UserRepository,
    private val localRepository: LocalRepository,
) : ViewModel() {
    /***** variables *****/

    // badge
    var isFirstBadge: Boolean = true
    val badgeName = MutableLiveData<String>()
    val badgeImageUrl = MutableLiveData<String>()

    // diary
    private val _diaryDateList: MutableStateFlow<List<LocalDate>> = MutableStateFlow(emptyList())

    private val _diaryList: MutableLiveData<DiarySummary?> = MutableLiveData()
    val diaryList: LiveData<DiarySummary?>
        get() = _diaryList

    // calendar
    private val _visibleDates =
        MutableStateFlow(
            calculateWeeklyCalendarDays(
                startDate = LocalDate.now().getWeekStartDate().minusWeeks(1),
                emptyList(),
            ),
        )
    val visibleDates: StateFlow<Array<List<Date>>> = _visibleDates

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val currentMonth: StateFlow<YearMonth>
        get() =
            isCalendarExpanded
                .zip(visibleDates) { isExpanded, dates ->
                    when {
                        isExpanded -> dates[PRESENT][dates[PRESENT].size / 2].day.toYearMonth()
                        dates[PRESENT].count { it.day.month == dates[PRESENT][FIRST_IN_ARRAY].day.month } > 3 ->
                            dates[PRESENT][FIRST_IN_ARRAY]
                                .day
                                .toYearMonth()

                        else -> dates[PRESENT][dates[PRESENT].size - 1].day.toYearMonth()
                    }
                }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now().toYearMonth())

    private val _isCalendarExpanded = MutableStateFlow(false)
    val isCalendarExpanded: StateFlow<Boolean> = _isCalendarExpanded

    // firebase remote config
    private val _configInfo = MutableStateFlow(ConfigInfo())
    val configInfo: StateFlow<ConfigInfo> = _configInfo

    // banner
    private val _isBannerVisible = MutableStateFlow(true)
    val isBannerVisible: StateFlow<Boolean> = _isBannerVisible

    /***** init *****/

    init {
        fetchRemoteConfig()
        observeBannerState()
    }

    /***** functions *****/

    // diary
    private suspend fun getDates(
        startDate: LocalDate,
        period: Period,
    ): List<LocalDate> {
        var diaryDates: List<LocalDate> = emptyList()
        val endDate =
            when (period) {
                Period.WEEK -> startDate.plusDays(END_DATE_AFTER_THREE_WEEKS)
                Period.MONTH -> startDate.plusMonths(START_DATE_AFTER_THREE_MONTHS).minusDays(1)
            }
        val startAsString = DateUtil.WithServer.asStringOnlyDate(startDate)
        val endAsString = DateUtil.WithServer.asStringOnlyDate(endDate)

        return viewModelScope
            .async {
                try {
                    diaryRepository
                        .getDiaries(startAsString, endAsString)
                        .run { diaryDates = data().diaries.keys.toList() }
                } catch (t: Throwable) {
                    Timber.e(t)
                }
                diaryDates
            }.await()
    }

    private suspend fun getDateDiary(date: LocalDate) {
        val dateAsString = DateUtil.WithServer.asStringOnlyDate(date)

        try {
            diaryRepository
                .getDiaries(start = dateAsString, end = dateAsString)
                .run {
                    _diaryList.postValue(
                        data().diaries.values.firstOrNull()?.let { dto ->
                            DiarySummary(
                                id = dto.id,
                                content = dto.content,
                                createdAt = dto.createdAt,
                            )
                        },
                    )
                }
        } catch (t: Throwable) {
            Timber.e(t)
        }
    }

    fun setBadgeInfo(
        name: String,
        imageUrl: String,
        isFirst: Boolean,
    ) {
        badgeName.value = name
        badgeImageUrl.value = imageUrl
        isFirstBadge = isFirst
    }

    // calendar
    fun onStateChange(state: CalendarState) {
        when (state) {
            CalendarState.ExpandCalendar -> {
                calculateCalendarDates(
                    startDate = currentMonth.value.minusMonths(1).atDay(FIRST),
                    period = Period.MONTH,
                )
                _isCalendarExpanded.value = true
                sendEvent(AmplitudeEventType.FULL_CALENDAR_APPEAR)
            }

            CalendarState.CollapseCalendar -> {
                calculateCalendarDates(
                    startDate =
                    calculateWeeklyCalendarVisibleStartDay()
                        .getWeekStartDate()
                        .minusWeeks(1),
                    period = Period.WEEK,
                )
                _isCalendarExpanded.value = false
            }

            is CalendarState.LoadNextDates -> {
                calculateCalendarDates(state.startDate, state.period)
            }

            is CalendarState.SelectDate -> {
                viewModelScope.launch {
                    getDateDiary(state.date)
                    _selectedDate.emit(state.date)
                }
            }
        }
    }

    private fun calculateCalendarDates(
        startDate: LocalDate,
        period: Period = Period.WEEK,
    ) {
        viewModelScope.launch {
            val diaryDateList =
                when (period) {
                    Period.WEEK -> getDates(startDate, Period.WEEK)
                    Period.MONTH -> getDates(startDate, Period.MONTH)
                }

            val visibleDates =
                when (period) {
                    Period.WEEK -> calculateWeeklyCalendarDays(startDate, diaryDateList)
                    Period.MONTH -> calculateMonthlyCalendarDays(startDate, diaryDateList)
                }

            withContext(Dispatchers.Main) {
                _diaryDateList.emit(diaryDateList)
                _visibleDates.emit(visibleDates)
            }
        }
    }

    private fun calculateWeeklyCalendarVisibleStartDay(): LocalDate {
        val halfOfMonth = visibleDates.value[PRESENT][visibleDates.value[PRESENT].size / 2]
        val visibleMonth = YearMonth.of(halfOfMonth.day.year, halfOfMonth.day.month)
        return if (selectedDate.value.month == visibleMonth.month && selectedDate.value.year == visibleMonth.year) {
            selectedDate.value
        } else {
            visibleMonth.atDay(FIRST)
        }
    }

    private fun calculateWeeklyCalendarDays(
        startDate: LocalDate,
        diaryDates: List<LocalDate>,
    ): Array<List<Date>> {
        val dateList = mutableListOf<Date>()

        startDate.getNextDates(THREE_WEEKS).forEach {
            dateList.add(Date(it, true, diaryDates.contains(it)))
        }

        return Array(DATELIST_SIZE) {
            dateList.slice(it * 7 until (it + 1) * 7)
        }
    }

    private fun calculateMonthlyCalendarDays(
        startDate: LocalDate,
        diaryDates: List<LocalDate>,
    ): Array<List<Date>> =
        Array(DATELIST_SIZE) { monthIndex ->
            val monthFirstDate = startDate.plusMonths(monthIndex.toLong())
            val monthLastDate = monthFirstDate.plusMonths(1).minusDays(1)

            monthFirstDate.getWeekStartDate().let { weekBeginningDate ->
                if (weekBeginningDate != monthFirstDate) {
                    weekBeginningDate.getRemainingDatesInMonth().map {
                        Date(it, false, diaryDates.contains(it))
                    }
                } else {
                    listOf()
                } +
                        monthFirstDate
                            .getNextDates(monthFirstDate.month.length(monthFirstDate.isLeapYear))
                            .map {
                                Date(it, true, diaryDates.contains(it))
                            } +
                        monthLastDate.getRemainingDatesInWeek().map {
                            Date(it, false, diaryDates.contains(it))
                        }
            }
        }

    private fun sendEvent(event: AmplitudeEventType) {
        try {
            viewModelScope.launch {
                Smeem.AMPLITUDE.track(event.eventName)
            }
        } catch (t: Throwable) {
            // 이벤트 발송이 기존 로직에 영향은 없도록
            Timber.tag("AMPLITUDE").e("amplitude send error!")
        }
    }

    fun activeVisit(onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                userRepository.activeVisit()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    private suspend fun updateBannerClosed() =
        coroutineScope {
            launch {
                localRepository.setBooleanValue(
                    BANNER_CLOSED,
                    true,
                )
            }
        }

    fun closeBanner() {
        viewModelScope.launch {
            updateBannerClosed()
            _isBannerVisible.value = false
        }
    }

    /**
     * Firebase Remote Config 에서 데이터를 가져옴
     * 로컬에 저장된 배너 버전과 비교하여 새로운 배너가 있을 경우
     * 배너를 보여주고, 로컬에 저장된 버전을 업데이트
     */
    private fun fetchRemoteConfig() {
        try {
            getRemoteConfigInstance().fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val newConfigInfo =
                        ConfigInfo(
                            bannerVersion =
                            getRemoteConfigInstance()
                                .getLong("banner_version")
                                .toInt(),
                            bannerTitle = getRemoteConfigInstance().getString("banner_title"),
                            bannerContent = getRemoteConfigInstance().getString("banner_content"),
                            isBannerEnabled = getRemoteConfigInstance().getBoolean("is_banner_enabled"),
                            isExternalEvent = getRemoteConfigInstance().getBoolean("is_external_event"),
                            bannerEventPath = getRemoteConfigInstance().getString("banner_event_path"),
                        )

                    viewModelScope.launch {
                        val savedBannerVersion = localRepository.getIntValue("banner_version")
                        val isBannerClosed = localRepository.getBooleanValue("banner_closed")

                        if (newConfigInfo.bannerVersion > savedBannerVersion) {
                            localRepository.setIntValue(
                                BANNER_VERSION,
                                newConfigInfo.bannerVersion,
                            )
                            localRepository.setBooleanValue(BANNER_CLOSED, false)
                            _isBannerVisible.value = newConfigInfo.isBannerEnabled
                        } else {
                            _isBannerVisible.value =
                                newConfigInfo.isBannerEnabled &&
                                        !isBannerClosed
                        }
                    }

                    _configInfo.value = newConfigInfo
                } else {
                    Timber.tag("REMOTE_CONFIG").e("fetch failed")
                }
            }
        } catch (e: Exception) {
            Timber.tag("REMOTE_CONFIG").e(e)
        }
    }

    private fun observeBannerState() {
        viewModelScope.launch {
            localRepository.getBooleanFlow("banner_closed").collect { isClosed ->
                val config = _configInfo.value
                _isBannerVisible.value = config.isBannerEnabled && !isClosed
            }
        }
    }

    companion object {
        const val PRESENT = 1
        const val FIRST_IN_ARRAY = 0
        const val FIRST = 1
        const val END_DATE_AFTER_THREE_WEEKS: Long = 20
        const val START_DATE_AFTER_THREE_MONTHS: Long = 3
        const val THREE_WEEKS = 21
        const val DATELIST_SIZE = 3

        private val remoteConfig by lazy {
            Firebase.remoteConfig.apply {
                setConfigSettingsAsync(
                    remoteConfigSettings {
                        minimumFetchIntervalInSeconds = 0
                    },
                )
            }
        }

        fun getRemoteConfigInstance(): FirebaseRemoteConfig = remoteConfig
    }
}
