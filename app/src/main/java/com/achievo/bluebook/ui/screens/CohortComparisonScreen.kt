package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.CohortSlice
import com.achievo.bluebook.data.CohortStanding
import com.achievo.bluebook.data.cohortStanding
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*

/** FR22: how other (anonymous) readers of this book are doing on the same topics. Reads only a
 *  cohort MEAN (topic_cohort, gated by BR09's minimum-attempts threshold) — never another
 *  profile's identity or raw data. */
@Composable
fun CohortComparisonScreen(vm: AppViewModel, subject: String, onBack: () -> Unit) {
    LaunchedEffect(Unit) {
        if (vm.cohortSlices.isEmpty() && !vm.cohortLoading) vm.loadCohort()
    }

    vm.cohortError?.let { msg ->
        NoticeDialog(
            title = "COULDN'T LOAD COMPARISON",
            body = msg,
            confirm = "Retry", onConfirm = { vm.dismissCohortError(); vm.loadCohort() },
            dismiss = "Close", onDismiss = { vm.dismissCohortError() }
        )
    }

    val slices = vm.cohortSlices.filter { it.subjectLabel == subject }

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        Text(
            "vs. other readers of this book", fontSize = 13.sp, color = Muted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vm.cohortLoading) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Maroon) } }
            } else if (slices.isEmpty()) {
                item { Text("No cohort data for this subject yet.", color = Muted) }
            }
            items(slices) { slice -> CohortRow(slice) }
        }
    }
}

@Composable
private fun CohortRow(slice: CohortSlice) {
    val standing = cohortStanding(slice)
    val (label, color) = when (standing) {
        CohortStanding.ABOVE -> "Above average" to CorrectGreen
        CohortStanding.BELOW -> "Below average" to WrongRed
        CohortStanding.NEAR -> "Near average" to FlagAmber
        CohortStanding.UNKNOWN -> "Not enough data yet" to Muted
    }
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(slice.topicLabel, fontWeight = FontWeight.SemiBold)
                Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("You: ${slice.ownPercent}%", fontFamily = Mono, fontWeight = FontWeight.Bold)
                Text(
                    if (slice.cohortMasteryPercent != null) "Cohort: ${slice.cohortMasteryPercent}%" else "Cohort: —",
                    fontSize = 11.sp, color = Muted
                )
            }
        }
    }
}
