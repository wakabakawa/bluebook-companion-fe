package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.estimatedScoreRange
import com.achievo.bluebook.data.overallMastery
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.AppCard
import com.achievo.bluebook.ui.components.BluebookTopBar
import com.achievo.bluebook.ui.theme.Maroon
import com.achievo.bluebook.ui.theme.Mono
import com.achievo.bluebook.ui.theme.Muted

/** FR20: an estimated score range against the target program, with the gap shown — a placeholder
 *  linear mapping (CC11), not a real psychometric model, pending Achievo's actual passing-score
 *  cutoffs per program/exam. Never presented as authoritative; the band/cutoff are clearly labeled. */
@Composable
fun EstimatedScoreScreen(vm: AppViewModel, onBack: () -> Unit) {
    val overall = overallMastery(vm.attempts)
    val estimate = estimatedScoreRange(overall.percent)
    val targetCourse = vm.profile.course.ifBlank { null }

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                Text("Estimated Entrance Test Score", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (overall.answered == 0) {
                    Text("Answer a sheet to see an estimate here.", color = Muted)
                } else {
                    Text("${estimate.low}–${estimate.high}", fontFamily = Mono, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Maroon)
                    Text(
                        if (estimate.gap > 0) "${estimate.gap} points below the default target cutoff" else "At or above the default target cutoff",
                        color = Muted, fontSize = 13.sp
                    )
                    targetCourse?.let { Text("Target: $it", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
            Text(
                "This is a placeholder estimate from your overall mastery (${overall.percent}%, based on ${overall.answered} items), " +
                    "not a real score prediction. Achievo Institute hasn't yet supplied actual passing-score cutoffs per program or " +
                    "entrance exam — once they do, this screen will use real cutoffs instead of the default band.",
                fontSize = 12.sp, color = Muted, textAlign = TextAlign.Start
            )
        }
    }
}
