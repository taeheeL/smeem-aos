package com.sopt.smeem.presentation.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sopt.smeem.databinding.BottomSheetSignUpBinding
import com.sopt.smeem.domain.common.logging
import com.sopt.smeem.domain.model.SocialType
import com.sopt.smeem.presentation.auth.KakaoHandler
import com.sopt.smeem.presentation.auth.LoginProcess
import com.sopt.smeem.util.setOnSingleClickListener
import timber.log.Timber

class SignUpBottomSheet() : BottomSheetDialogFragment(), LoginProcess {
    var _binding: BottomSheetSignUpBinding? = null
    private val binding: BottomSheetSignUpBinding
        get() = requireNotNull(_binding)
    private val vm: OnBoardingVM by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        binding.btnKakao.setOnSingleClickListener {

            if (KakaoHandler.isAppEnabled(context)) {
                KakaoHandler.loginOnApp(context,
                    onSuccess = { kakaoAccessToken, kakaoRefreshToken ->
                        vm.loadingStart()

                        vm.login(
                            kakaoAccessToken = kakaoAccessToken,
                            kakaoRefreshToken = kakaoRefreshToken,
                            socialType = SocialType.KAKAO,
                            onError = { e -> Timber.e("LOGIN_FAILED") }
                        )
                    },
                    onFailed = { exception -> exception.logging("KAKAO_LOGIN") })
            } else {
                KakaoHandler.loginOnWeb(
                    context,
                    onSuccess = { kakaoAccessToken, kakaoRefreshToken ->
                        vm.loadingStart()

                        vm.login(
                            kakaoAccessToken = kakaoAccessToken,
                            kakaoRefreshToken = kakaoRefreshToken,
                            socialType = SocialType.KAKAO,
                            onError = { e -> Timber.e("LOGIN_FAILED") }
                        )
                    },
                    onFailed = { exception -> exception.logging("KAKAO_LOGIN") })
            }
        }

//        binding.tvSignUpAnonymous.setOnSingleClickListener {
//            vm.goAnonymous()
//        }
    }

    override fun onDestroy() {
        _binding = null
        vm.loadingEnd()
        super.onDestroy()
    }

    companion object {
        const val TAG = "BottomSheetSignUp"
    }
}