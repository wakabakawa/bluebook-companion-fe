package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.AppConfig
import com.achievo.bluebook.data.PlanStatus
import com.achievo.bluebook.data.ReviewPlanEntry
import com.achievo.bluebook.data.TopicStat
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*
import kotlin.math.roundToInt

/** FR21: fetches and shows a book-wide review plan from the backend optimizer. */
@Composable
fun ReviewPlanScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) {
        if (vm.reviewPlan == null && !vm.reviewPlanLoading) vm.loadReviewPlan()
    }

    vm.reviewPlanError?.let { msg ->
        NoticeDialog(
            title = "COULDN'T BUILD PLAN",
            body = msg,
            confirm = "Retry", onConfirm = { vm.dismissReviewPlanError(); vm.loadReviewPlan() },
            dismiss = "Close", onDismiss = { vm.dismissReviewPlanError() }
        )
    }

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                AppCard(color = CardPink) {
                    Text("Time budget", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    BudgetStepper(vm.reviewBudgetMin, vm::setReviewBudget)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton("Build plan", { vm.loadReviewPlan() }, Modifier.fillMaxWidth(), enabled = !vm.reviewPlanLoading)
                }
            }

            if (vm.reviewPlanLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Maroon)
                    }
                }
            }

            val plan = vm.reviewPlan
            if (plan != null) {
                item {
                    LabeledBar(
                        "Budget used",
                        if (plan.budgetMin == 0) null else (plan.totalCostMin * 100 / plan.budgetMin).coerceAtMost(100),
                        "${plan.totalCostMin} of ${plan.budgetMin} minutes • ${plan.entries.size} topics",
                        CorrectGreen
                    )
                }

                when (plan.status) {
                    PlanStatus.OPTIMAL_RELAXED_Q -> item {
                        Text(
                            "Only ${plan.qUsed} of ${plan.qRequested} strong topics could be fitted.",
                            fontSize = 12.sp, color = FlagAmber, fontWeight = FontWeight.SemiBold
                        )
                    }
                    PlanStatus.BUDGET_TOO_SMALL -> item {
                        Text(
                            "No topic fits in ${plan.budgetMin} minutes. Try raising the budget.",
                            fontSize = 14.sp, color = Muted
                        )
                    }
                    PlanStatus.EMPTY_INPUT -> item {
                        Text("No published topics to plan from yet.", fontSize = 14.sp, color = Muted)
                    }
                    else -> {}
                }

                if (plan.entries.isNotEmpty()) {
                    item {
                        AppCard(color = CardPink) {
                            Text("Your review plan:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = Maroon, thickness = 3.dp, modifier = Modifier.padding(vertical = 6.dp))
                            plan.entries.forEach { e -> PlanRow(e, vm.reviewTopics) }
                        }
                    }
                }

                item {
                    val exactness = if (plan.exact) "exact solution" else "approximate solution"
                    Text(
                        listOfNotNull(plan.engineLabel, exactness).joinToString(" • "),
                        fontSize = 11.sp, color = Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetStepper(value: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(value - AppConfig.BUDGET_STEP_MIN) }) {
            Icon(Icons.Outlined.Remove, contentDescription = "Decrease budget")
        }
        Text("$value min", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = { onChange(value + AppConfig.BUDGET_STEP_MIN) }) {
            Icon(Icons.Outlined.Add, contentDescription = "Increase budget")
        }
    }
}

@Composable
private fun PlanRow(e: ReviewPlanEntry, topics: List<TopicStat>) {
    val topic = topics.firstOrNull { it.topicId == e.topicId }
    val label = topic?.let { "${it.branch}: ${it.topic}" } ?: e.topicId
    val recency = topic?.daysSincePractice?.let { "last practised ${it}d ago" } ?: "not practised yet"
    val caption = listOfNotNull(
        topic?.bookPageRef?.let { "Pg $it" },
        "${e.costMin} min",
        recency
    ).joinToString(" | ")

    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${e.rank}.", color = Maroon, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(caption, fontSize = 11.sp, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (e.strong) TagBadge("STRONG", CorrectGreen, CorrectBg)
                if (!e.reliable) TagBadge("LOW DATA", FlagAmber, FlagBg)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${(e.mastery * 100).roundToInt()}%", color = Maroon, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${(e.staleness * 100).roundToInt()}% stale", fontSize = 11.sp, color = Muted)
        }
    }
}

@Composable
private fun TagBadge(text: String, fg: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color) {
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.border(1.dp, fg, shape).background(bg, shape).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
