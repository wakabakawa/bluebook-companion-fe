package com.achievo.bluebook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.ScoreResult
import com.achievo.bluebook.data.Sheet
import com.achievo.bluebook.ui.theme.*

// ---------------------------------------------------------------- chrome

enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Outlined.Home),
    SETS("sets", "Answer Sheet", Icons.Outlined.ListAlt),
    RESULTS("results", "Results", Icons.Outlined.AssignmentTurnedIn),
    INFO("info", "Info", Icons.Outlined.Info),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings)
}

@Composable
fun BottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        Tab.values().forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SelectBlue,
                    selectedTextColor = SelectBlue,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

/** TODO: replace the text placeholder with the Bluebook logo drawable once the client sends assets (CC12). */
@Composable
fun BluebookTopBar(
    onBack: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().background(Maroon).height(56.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("the MAROON BLUEBOOK", color = Maroon, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium) }
        }
        if (onBack != null) {
            TextButton(onClick = onBack) { Text("BACK", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

// ---------------------------------------------------------------- buttons, cards

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Maroon)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Maroon),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Maroon)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier.fillMaxWidth().shadow(3.dp, shape).clip(shape).background(color)
    Column((if (onClick != null) base.clickable(onClick = onClick) else base).padding(16.dp), content = content)
}

@Composable
fun CountChip(count: Int, label: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.border(1.5.dp, fg, shape).background(bg, shape).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$count", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = fg, fontSize = 11.sp)
    }
}

/** Score card used on Home, Results list and Results. Blank is shown separately (FR10, BR05). */
@Composable
fun ResultSummaryCard(sheet: Sheet, score: ScoreResult, dateLabel: String, timeLabel: String, onClick: (() -> Unit)? = null) {
    AppCard(color = CardPink, onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("${score.correct}/${score.total}", fontFamily = Mono, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
                Text(dateLabel, fontSize = 11.sp, color = Muted)
                Text("${score.percent}% Readiness", fontFamily = Mono, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Muted)
            }
        }
        HorizontalDivider(color = Maroon, thickness = 2.dp)
        Text("${sheet.branch.uppercase()} - ${sheet.setName.uppercase()}  •  $timeLabel", fontSize = 11.sp, color = Muted)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountChip(score.correct, "Correct", CorrectGreen, CorrectBg, Modifier.weight(1f))
            CountChip(score.wrong, "Wrong", WrongRed, WrongBg, Modifier.weight(1f))
            CountChip(score.blank, "Blank", BlankBrown, FlagBg, Modifier.weight(1f))
            CountChip(score.flagged, "Flagged", FlagAmber, FlagBg, Modifier.weight(1f))
        }
    }
}

/** Bar with "Based on N items" (NFR07: every figure states its basis). percent == null draws an empty bar. */
@Composable
fun LabeledBar(label: String, percent: Int?, caption: String, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (percent != null) Text("$percent%", fontFamily = Mono, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Muted)
        }
        LinearProgressIndicator(
            progress = { (percent ?: 0) / 100f },
            modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
            color = color,
            trackColor = TrackGray
        )
        Text(caption, fontSize = 12.sp, color = Muted)
    }
}

// ---------------------------------------------------------------- answer rows

@Composable
fun Bubble(
    letter: Char,
    filled: Boolean,
    fill: Color = SelectBlue,
    onClick: (() -> Unit)? = null,
    badge: Pair<ImageVector, Color>? = null
) {
    Box(
        Modifier.size(44.dp)   // NFR07: tap target big enough for rapid selection
            .clip(CircleShape)
            .background(if (filled) fill else BubbleFill)
            .border(1.5.dp, if (filled) fill else BubbleOutline, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(letter.toString(), color = if (filled) Color.White else BubbleOutline, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (badge != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape)
                    .background(badge.second).border(1.dp, Color.White, CircleShape)
            ) {
                Icon(badge.first, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp).align(Alignment.Center))
            }
        }
    }
}

@Composable
fun AnswerInputRow(number: Int, selected: Char?, flagged: Boolean, onPick: (Char) -> Unit, onFlag: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().shadow(2.dp, shape).clip(shape)
            .background(if (flagged) FlagBg else Color.White)
            .then(if (flagged) Modifier.border(2.dp, FlagAmber, shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$number.", fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.width(28.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf('A', 'B', 'C', 'D').forEach { c -> Bubble(c, selected == c, onClick = { onPick(c) }) }
        }
        IconButton(onClick = onFlag) {
            Icon(
                if (flagged) Icons.Filled.Flag else Icons.Outlined.Flag,
                contentDescription = "Flag item $number",
                tint = FlagAmber
            )
        }
    }
}

/** Review row (FR11). The tag line is an addition to the design: FR11 requires tags and the book page. */
@Composable
fun AnswerReviewRow(number: Int, chosen: Char?, correct: Char, flagged: Boolean, tagLine: String) {
    val shape = RoundedCornerShape(14.dp)
    val tone = when {
        chosen == null -> FlagAmber
        chosen == correct -> CorrectGreen
        else -> WrongRed
    }
    val bg = when {
        chosen == null -> FlagBg
        chosen == correct -> CorrectBg
        else -> WrongBg
    }
    Column(Modifier.fillMaxWidth().clip(shape).background(bg).border(2.dp, tone, shape).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$number.", fontFamily = Mono, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf('A', 'B', 'C', 'D').forEach { c ->
                    val badge: Pair<ImageVector, Color>? = when {
                        c == correct -> Icons.Filled.Check to CorrectGreen
                        c == chosen -> Icons.Filled.Close to WrongRed
                        else -> null
                    }
                    Bubble(c, filled = chosen != null, fill = tone, badge = badge)
                }
            }
            Icon(
                if (flagged) Icons.Filled.Flag else Icons.Outlined.Flag,
                contentDescription = if (flagged) "Flagged" else null,
                tint = FlagAmber,
                modifier = Modifier.padding(12.dp)
            )
        }
        Text(tagLine, fontSize = 11.sp, color = Muted)
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
fun NoticeDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    dismiss: String? = null,
    onDismiss: (() -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null
) {
    val dismissContent: (@Composable () -> Unit)? =
        if (dismiss != null && onDismiss != null) { @Composable { OutlineButton(dismiss, onDismiss) } } else null
    AlertDialog(
        onDismissRequest = {},
        containerColor = AppBg,
        title = {
            Text(title, Modifier.fillMaxWidth(), color = FlagAmber, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, textAlign = TextAlign.Center)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(body, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                extra?.invoke()
            }
        },
        confirmButton = { PrimaryButton(confirm, onConfirm) },
        dismissButton = dismissContent
    )
}
