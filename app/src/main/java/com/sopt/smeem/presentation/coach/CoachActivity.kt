package com.sopt.smeem.presentation.coach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.sopt.smeem.domain.dto.RetrievedBadgeDto
import com.sopt.smeem.presentation.EventVM
import com.sopt.smeem.presentation.IntentConstants.DIARY_CONTENT
import com.sopt.smeem.presentation.IntentConstants.DIARY_ID
import com.sopt.smeem.presentation.IntentConstants.RETRIEVED_BADGE_DTO
import com.sopt.smeem.presentation.IntentConstants.SNACKBAR_TEXT
import com.sopt.smeem.presentation.base.DefaultSnackBar
import com.sopt.smeem.presentation.coach.navigation.CoachNavGraph
import com.sopt.smeem.presentation.compose.theme.SmeemTheme
import com.sopt.smeem.presentation.home.HomeActivity
import com.sopt.smeem.util.getParcelableArrayListExtraCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoachActivity : ComponentActivity() {
    private val viewModel by viewModels<CoachViewModel>()
    private val eventVm by viewModels<EventVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val diaryContent = intent.getStringExtra(DIARY_CONTENT) ?: ""
        val diaryId = intent.getLongExtra(DIARY_ID, -1)
        viewModel.initialize(diaryId, diaryContent)

        setContent {
            SmeemTheme {
                val navController = rememberNavController()

                CoachNavGraph(
                    viewModel = viewModel,
                    eventVm = eventVm,
                    navController = navController,
                    onCloseClick = {
                        Intent(this, HomeActivity::class.java).apply {
                            putParcelableArrayListExtra(
                                RETRIEVED_BADGE_DTO,
                                ArrayList(
                                    intent.getParcelableArrayListExtraCompat<RetrievedBadgeDto>(
                                        RETRIEVED_BADGE_DTO
                                    ) ?: emptyList()
                                )
                            )
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }.run(::startActivity)
                    }
                )
            }
        }

        showDiaryCompleted()
    }

    private fun showDiaryCompleted() {
        val msg = intent.getStringExtra(SNACKBAR_TEXT)
        if (msg != null) {
            DefaultSnackBar.make(findViewById(android.R.id.content), msg).show()
            intent.removeExtra(SNACKBAR_TEXT)
        }
    }
}
