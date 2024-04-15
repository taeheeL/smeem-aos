package com.sopt.smeem.presentation.mypage.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sopt.smeem.presentation.mypage.TempMyPageActivity
import com.sopt.smeem.presentation.mypage.components.MySummaryTopAppBar
import com.sopt.smeem.presentation.mypage.components.OnlyBackArrowTopAppBar
import com.sopt.smeem.presentation.mypage.components.SettingTopAppBar
import com.sopt.smeem.presentation.mypage.more.MoreScreen
import com.sopt.smeem.presentation.mypage.mysummary.MySummaryScreen
import com.sopt.smeem.presentation.mypage.setting.SettingScreen

@Composable
fun MyPageNavHost(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    Scaffold(
        topBar = {
            when (currentRoute) {
                MyPageScreen.MySummary.route -> MySummaryTopAppBar(
                    onNavigationIconClick = { if (context is TempMyPageActivity) context.finish() },
                    onSettingClick = {
                        navController.navigate(MyPageScreen.Setting.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                MyPageScreen.Setting.route -> SettingTopAppBar(
                    onNavigationIconClick = { navController.popBackStack() },
                    onMoreClick = {
                        navController.navigate(MyPageScreen.More.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                MyPageScreen.More.route -> OnlyBackArrowTopAppBar(
                    onNavigationIconClick = { navController.popBackStack() }
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = MyPageScreen.MySummary.route) {
            addMySummary(navController = navController, modifier = Modifier.padding(it))
            addSetting(navController = navController, modifier = Modifier.padding(it))
            addMore(navController = navController, modifier = Modifier.padding(it))
        }
    }
}

private fun NavGraphBuilder.addMySummary(navController: NavController, modifier: Modifier) {
    composable(route = MyPageScreen.MySummary.route) {
        MySummaryScreen(
            navController = navController,
            modifier = modifier
        )
    }
}

private fun NavGraphBuilder.addSetting(navController: NavController, modifier: Modifier) {
    composable(route = MyPageScreen.Setting.route) {
        SettingScreen(
            navController = navController,
            modifier = modifier
        )
    }
}

private fun NavGraphBuilder.addMore(navController: NavController, modifier: Modifier) {
    composable(route = MyPageScreen.More.route) {
        MoreScreen(
            navController = navController,
            modifier = modifier
        )
    }
}