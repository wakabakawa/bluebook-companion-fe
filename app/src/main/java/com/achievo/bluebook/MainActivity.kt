package com.achievo.bluebook

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
                    onOpenResult = { nav.navigate("result/$it") }
                )
            }
            composable("sets") { SetsScreen(vm) { nav.navigate("sheet/$it") } }
            composable("results") { ResultsListScreen(vm) { nav.navigate("result/$it") } }
            composable("info") { InfoScreen() }
            composable("settings") { SettingsScreen(vm) }

            composable("sheet/{id}") { e ->
                AnswerSheetScreen(
                    vm, e.arguments?.getString("id") ?: "",
                    onExit = { nav.popBackStack() },
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
                    onKey = { nav.navigate("review/$id") },
                    onRetake = { sheet ->
                        vm.startSheet(sheet)
                        nav.navigate("sheet/${sheet.id}")
                    }
                )
            }
            composable("mastery/{id}") { e -> MasteryScreen(vm, e.arguments?.getString("id") ?: "") { nav.popBackStack() } }
            composable("review/{id}") { e -> ReviewScreen(vm, e.arguments?.getString("id") ?: "") { nav.popBackStack() } }
        }
    }
}
