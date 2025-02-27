package com.sopt.smeem.presentation.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.sopt.smeem.R
import com.sopt.smeem.databinding.ActivityOnBoardingBinding
import com.sopt.smeem.domain.model.TrainingGoalType
import com.sopt.smeem.event.AmplitudeEventType.ON_BOARDING_ALARM_VIEW
import com.sopt.smeem.event.AmplitudeEventType.ON_BOARDING_GOAL_VIEW
import com.sopt.smeem.event.AmplitudeEventType.ON_BOARDING_PLAN_VIEW
import com.sopt.smeem.event.AmplitudeEventType.SIGN_UP_SUCCESS
import com.sopt.smeem.presentation.EventVM
import com.sopt.smeem.presentation.base.BindingActivity
import com.sopt.smeem.presentation.home.HomeActivity
import com.sopt.smeem.presentation.join.JoinConstant.ACCESS_TOKEN
import com.sopt.smeem.presentation.join.JoinConstant.REFRESH_TOKEN
import com.sopt.smeem.presentation.join.JoinWithNicknameActivity
import com.sopt.smeem.presentation.splash.SplashLoginActivity
import com.sopt.smeem.util.setOnSingleClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnBoardingActivity :
    BindingActivity<ActivityOnBoardingBinding>(R.layout.activity_on_boarding) {
    private val vm: OnBoardingVM by viewModels()
    lateinit var bs: SignUpBottomSheet
    private val eventVm: EventVM by viewModels()

    override fun constructLayout() {
        super.constructLayout()
        setUpFragments()
        setUpBottomSheet()
        setUpLoading()
    }

    override fun addListeners() {
        onTouchNext()
        onSetTimeLater()
    }

    override fun addObservers() {
        observeStepChanging()
        observeOnStep1()
        observeOnStep2()
        observeOnStep3()
        observeLoading()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        vm.backStep()
    }

    private fun setUpFragments() {
        binding.tvOnBoardingHeaderNo.text = "1"
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_on_boarding, SettingGoalFragment())
            .commit()
    }

    private fun setUpBottomSheet() {
        bs = SignUpBottomSheet()
    }

    private fun setUpLoading() {
        binding.progressCircleOnBoardingLoading.bringToFront()
        binding.progressCircleOnBoardingLoading.isIndeterminate = false
    }

    private fun onTouchNext() {
        binding.btnOnBoardingNext.setOnSingleClickListener {
            vm.nextStep() // next step 으로 이동
        }
    }

    private fun observeStepChanging() {
        vm.step.observe(this@OnBoardingActivity) { step ->
            when (step) {
                0 -> {
                    Intent(this, SplashLoginActivity::class.java).run {
                        startActivity(this)
                        finish()
                    }
                }

                1 -> { // step 1 fragment => 트레이닝 목표 선택하기
                    if (vm.selectedGoal.value!!.selected) {
                        nextButtonOn()
                    }
                    setHeaderStepNo(1)
                    setHeaderTitle(resources.getText(R.string.on_boarding_goal_header_title))
                    setHeaderDescription(resources.getText(R.string.on_boarding_goal_header_description))

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fcv_on_boarding,
                            SettingGoalFragment()
                        )
                        .commit()

                    eventVm.sendEvent(ON_BOARDING_GOAL_VIEW)
                }

                2 -> { // step 2 fragment => 트레이닝 플랜 설정
                    nextButtonOff()
                    setHeaderStepNo(2)
                    setHeaderTitle(resources.getText(R.string.on_boarding_training_plan_header_title))
                    setHeaderDescription(resources.getText(R.string.on_boarding_training_plan_header_description))
                    setButtonTextNext()

                    vm.isDaysEmpty.value = false

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fcv_on_boarding,
                            TrainingPlanSettingFragment()
                        )
                        .commit()
                    eventVm.sendEvent(ON_BOARDING_PLAN_VIEW)
                }

                3 -> { // step 3 fragment => 알림 시간 설정하기
                    setHeaderStepNo(3)
                    setHeaderTitle(resources.getText(R.string.on_boarding_time_header_title))
                    setHeaderDescription(resources.getText(R.string.on_boarding_time_header_description))
                    setButtonTextComplete()

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fcv_on_boarding,
                            SettingTimeFragment()
                        )
                        .commit()

                    eventVm.sendEvent(ON_BOARDING_ALARM_VIEW)
                }

                4 -> { // step 4 : 알림 권한 체크 및 api token 가 local 에 있는지 (이전에 로그인했는지 check)
                    checkNotiPermission()
                }

                else -> {}
            }
        }
    }

    private fun observeOnStep1() {
        vm.selectedGoal.observe(this@OnBoardingActivity) {
            // 어떤 버튼값이라도 선택되어있으면 step2 로가는 next 를 활성화시킨다.
            if (it != TrainingGoalType.NO_SELECTED) {
                nextButtonOn()
            } else {
                nextButtonOff()
            }
        }
    }

    private fun observeOnStep2() {
        vm.selectedPlan.observe(this@OnBoardingActivity) {
            // 어떤 버튼값이라도 선택되어있으면 step2 로가는 next 를 활성화시킨다.
            if (it != TrainingPlanType.NOT_SELECTED) {
                nextButtonOn()
            } else {
                nextButtonOff()
            }
        }
    }

    private fun observeOnStep3() {
        observeSelectedDays()
        observeJoinOrAnonymous()
    }

    private fun observeLoading() {
        vm.onLoading.observe(this) {
            when (it) {
                LoadingState.NOT_STARTED -> {
                    binding.progressCircleOnBoardingLoading.isIndeterminate = false
                }

                LoadingState.ACT -> {
                    binding.progressCircleOnBoardingLoading.isIndeterminate = true
                }

                LoadingState.DONE -> {
                    binding.progressCircleOnBoardingLoading.isIndeterminate = false
                }
            }
        }
    }

    private fun setHeaderStepNo(no: Int) {
        binding.tvOnBoardingHeaderNo.text = no.toString()
    }

    private fun setHeaderTitle(title: CharSequence) {
        binding.tvOnBoardingHeaderTitleStatic.text = title
    }

    private fun setHeaderDescription(text: CharSequence) {
        binding.tvOnBoardingHeaderDescriptionStatic.text = text
    }

    private fun setButtonTextNext() {
        binding.btnOnBoardingNext.text = "다음"
    }

    private fun setButtonTextComplete() {
        binding.btnOnBoardingNext.text = "완료"
    }

    private fun onSetTimeLater() {
        vm.setTimeLater.observe(this) {
            if (it) { // true

                val accessTokenFromPast: String? = intent.getStringExtra(ACCESS_TOKEN)
                if (accessTokenFromPast != null) {
                    vm.sendPlanDataWithAuth(
                        token = accessTokenFromPast,
                        onSuccess = {
                            val toJoin = Intent(this, JoinWithNicknameActivity::class.java)
                            toJoin.putExtra(ACCESS_TOKEN, accessTokenFromPast)
                            toJoin.putExtra(REFRESH_TOKEN, intent.getStringExtra(REFRESH_TOKEN))
                            startActivity(toJoin)
                            if (!isFinishing) finish()
                        },
                        onError = { e ->
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    bs.show(supportFragmentManager, SignUpBottomSheet.TAG)
                }
            }
        }
    }

    private fun observeSelectedDays() {
        vm.isDaysEmpty.observe(this) {
            if (it) {
                nextButtonOff()
            } else if (vm.selectedPlan.value != TrainingPlanType.NOT_SELECTED) {
                nextButtonOn()
            }
        }
    }

    private fun nextButtonOn() {
        with(binding.btnOnBoardingNext) {
            setBackgroundColor(resources.getColor(R.color.point, null))
            isEnabled = true
        }
    }

    private fun nextButtonOff() {
        with(binding.btnOnBoardingNext) {
            setBackgroundColor(resources.getColor(R.color.point_inactive, null))
            setTextColor(resources.getColor(R.color.white, null))
            isEnabled = false
        }
    }

    private fun observeJoinOrAnonymous() {
        observerToGoAnonymous()
        observerToGoLogin()
    }

    private fun observerToGoAnonymous() {
        vm.goAnonymous.observe(this@OnBoardingActivity) { wantToBeAnonymous ->
            when (wantToBeAnonymous) {
                true -> {
                    vm.saveOnBoardingData()

                    // TODO : remove
                    Toast.makeText(this, "비회원 기능은 아직 동작하지 않습니다.", Toast.LENGTH_SHORT).show()
                    // gotoHome()
                }

                false -> { // TODO : 어떻게 해야하지? }
                }
            }
        }
    }

    private fun observerToGoLogin() {
        vm.loginResult.observe(this@OnBoardingActivity) { result ->
            when (result.isRegistered) {
                true -> {
                    vm.loadingEnd()
                    vm.saveTokenOnLocal(result.apiAccessToken, result.apiRefreshToken)
                    gotoHome()
                }

                false -> {
                    vm.sendPlanDataOnAnonymous(
                        onSuccess = {
                            val toJoin = Intent(
                                this@OnBoardingActivity,
                                JoinWithNicknameActivity::class.java
                            )
                            toJoin.putExtra(ACCESS_TOKEN, result.apiAccessToken)
                            toJoin.putExtra(REFRESH_TOKEN, result.apiRefreshToken)
                            startActivity(toJoin)

                            if (!isFinishing) finish()
                        },
                        onError = { t ->
                            Toast.makeText(
                                this@OnBoardingActivity, t.message, Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    eventVm.sendEvent(SIGN_UP_SUCCESS)

                }
            }
        }
    }


    private fun gotoHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun checkNotiPermission() {
        when {
            // 1. 알림 권한이 이미 허용되었을 때
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                vm.setNotiPermissionStatus(true)
                // 3/3 (트레이닝 시간 설정) 에서 로그인 바텀시트 띄우기전에 이미 kakao 로그인이 된 상태인지 확인
                checkAlreadyAuthed()
            }

            // 2. 사용자가 이전에 권한을 거부했을 때
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                vm.setNotiPermissionStatus(false)
                checkAlreadyAuthed()
            }

            // 3. 알림 권한을 처음으로 받는 것일 때
            else -> {
                notiPermissionResultCallback.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private val notiPermissionResultCallback =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                vm.setNotiPermissionStatus(false)
                // dialog 바깥쪽을 눌러 나간 경우 (허용, 거부 선택하지 않은 경우)
                // 바텀시트가 뜨지 않도록
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    checkAlreadyAuthed()
                }
            } else {
                vm.setNotiPermissionStatus(true)
                checkAlreadyAuthed()
            }
        }

    private fun checkAlreadyAuthed() {
        val accessTokenFromPast: String? = intent.getStringExtra(ACCESS_TOKEN)
        if (accessTokenFromPast != null) {
            // 이미 사전에 로그인을 수행했던 경우 ( case : "이미 계정이 있어요" 를 통한 온보딩 접근 )
            // hasPlan 이 false 여서 트레이닝 설정에 들어왔고, hasPlan 이 false 이면, isRegistered 도 false. (true 인 Case 는 없다.)
            vm.sendPlanDataWithAuth(
                token = accessTokenFromPast,
                onSuccess = {
                    val toJoin = Intent(this, JoinWithNicknameActivity::class.java)
                    toJoin.putExtra(ACCESS_TOKEN, accessTokenFromPast)
                    toJoin.putExtra(REFRESH_TOKEN, intent.getStringExtra(REFRESH_TOKEN))
                    startActivity(toJoin)
                    if (!isFinishing) finish()
                },
                onError = { e -> Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
            )
        }
        // 사전 로그인이 없었으면 login 동작하도록
        else {
            bs.show(supportFragmentManager, SignUpBottomSheet.TAG)
        }
    }
}