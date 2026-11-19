package com.achievo.bluebook

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.BottomBar
import com.achievo.bluebook.ui.components.Tab
import com.achievo.bluebook.ui.screens.*
import com.achievo.bluebook.ui.theme.AppBg
import com.achievo.bluebook.ui.theme.BluebookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BluebookTheme { BluebookApp() } }
    }
}

@Composable
fun BluebookApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBottom = Tab.values().any { it.route == route }

    fun goTab(r: String) {
        nav.navigate(r) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = AppBg,
        bottomBar = { if (showBottom) BottomBar(route) { goTab(it) } }
    ) { pad ->
        NavHost(nav, startDestination = "splash", modifier = Modifier.padding(pad)) {
            composable("splash") {
                SplashScreen { nav.navigate("home") { popUpTo("splash") { inclusive = true } } }
            }
            composable("home") {
                HomeScreen(
                    vm,
                    onStartNew = { goTab("sets") },
                    onContinue = { nav.navigate("sheet/$it") },
                    onOpenResult = { nav.navigate("result/$it") },
                    onReviewPlan = { nav.navigate("reviewplan") },
                    onEstimatedScore = { nav.navigate("estimatedscore") }
                )
            }
            composable("sets") { SetsScreen(vm) { nav.navigate("sheet/$it") } }
            composable("results") { ResultsListScreen(vm) { nav.navigate("result/$it") } }
            composable("info") { InfoScreen() }
            composable("settings") {
                SettingsScreen(
                    vm,
                    onOpenTerms = { nav.navigate("terms") },
                    onOpenPrivacy = { nav.navigate("privacy") }
                )
            }
            composable("terms") { TermsScreen { nav.popBackStack() } }
            composable("privacy") { PrivacyScreen { nav.popBackStack() } }

            composable("sheet/{id}") { e ->
                AnswerSheetScreen(
                    vm, e.arguments?.getString("id") ?: "",
                    // Always lands on Home, regardless of whether this sheet was opened from
                    // Home's "Continue?" card, the Answer Sheet tab, or a Retake - a draft is
                    // only ever resumed from Home, so that's the one sensible place to exit to.
                    onExit = { goTab("home") },
                    onSubmitted = { attemptId ->
                        nav.navigate("result/$attemptId") { popUpTo("sheet/{id}") { inclusive = true } }
                    }
                )
            }
            composable("result/{id}") { e ->
                val id = e.arguments?.getString("id") ?: ""
                ResultScreen(
                    vm, id,
                    onBack = { nav.popBackStack() },
                    onAnalysis = { nav.navigate("mastery/$id") },
                    onRetake = { sheet ->
                        vm.startSheet(sheet)
                        nav.navigate("sheet/${sheet.id}")
                    }
                )
            }
            composable("mastery/{id}") { e ->
                MasteryScreen(
                    vm, e.arguments?.getString("id") ?: "",
                    onReviewPlan = { nav.navigate("reviewplan") },
                    onCohortComparison = { subject -> nav.navigate("cohort/${Uri.encode(subject)}") },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("reviewplan") { ReviewPlanScreen(vm) { nav.popBackStack() } }
            composable("cohort/{subject}") { e ->
                CohortComparisonScreen(vm, e.arguments?.getString("subject") ?: "") { nav.popBackStack() }
            }
            composable("estimatedscore") { EstimatedScoreScreen(vm) { nav.popBackStack() } }
        }
    }
}
