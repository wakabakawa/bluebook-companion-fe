package com.achievo.bluebook.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.R
import com.achievo.bluebook.data.ProgressPoint
import com.achievo.bluebook.data.ScoreResult
import com.achievo.bluebook.data.Sheet
import com.achievo.bluebook.data.Slice
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

@Composable
fun BluebookTopBar(
    onBack: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().background(Maroon).height(76.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Image(
                painter = painterResource(R.drawable.logo_maroon_bluebook),
                contentDescription = "The Maroon Bluebook",
                modifier = Modifier.height(44.dp)
            )
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
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Maroon),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Maroon)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

/** A hold must last this long before [AppCard]'s onLongClick fires - longer than the system
 *  default so a hold-to-delete isn't triggered by an ordinary press-and-drag-to-scroll. */
private const val HOLD_TO_DELETE_MS = 2000L

private class LongHoldViewConfiguration(
    base: ViewConfiguration,
    override val longPressTimeoutMillis: Long
) : ViewConfiguration by base

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier.fillMaxWidth().shadow(3.dp, shape).clip(shape).background(color)
    val interactable = when {
        onLongClick != null -> base.combinedClickable(onLongClick = onLongClick, onClick = onClick ?: {})
        onClick != null -> base.clickable(onClick = onClick)
        else -> base
    }
    val card: @Composable () -> Unit = { Column(interactable.padding(16.dp), content = content) }
    if (onLongClick != null) {
        CompositionLocalProvider(
            LocalViewConfiguration provides LongHoldViewConfiguration(LocalViewConfiguration.current, HOLD_TO_DELETE_MS),
            content = card
        )
    } else {
        card()
    }
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

/** Score card used on Home, Results list and Results. Blank is shown separately (FR10, BR05).
 *  [onLongClick] is only wired up on Home, as the entry point for withdrawing the latest attempt. */
@Composable
fun ResultSummaryCard(
    sheet: Sheet, score: ScoreResult, dateLabel: String, timeLabel: String,
    onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null
) {
    AppCard(color = CardPink, onClick = onClick, onLongClick = onLongClick) {
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

/** Ranked weak/strong topic list, e.g. "Areas to improve" / "Your strengths" (FR13, FR18, used
 *  on Home and Mastery). [branch] prefixes each row's label when set (Home shows it across
 *  different sheets; Mastery already scopes to one sheet so omits it). */
@Composable
fun TopicRankCard(
    title: String,
    entries: List<Slice>,
    branch: String?,
    setName: String,
    cardColor: Color,
    accentColor: Color,
    footer: (@Composable () -> Unit)? = null
) {
    AppCard(color = cardColor, modifier = Modifier.padding(top = 8.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = accentColor, thickness = 3.dp, modifier = Modifier.padding(vertical = 6.dp))
        entries.forEachIndexed { i, t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}.", color = accentColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (branch != null) "$branch: ${t.label}" else t.label, fontWeight = FontWeight.SemiBold)
                    val subtitle = listOfNotNull(setName.ifBlank { null }, t.note).joinToString(" | ")
                    if (subtitle.isNotBlank()) Text(subtitle, fontSize = 11.sp, color = Muted)
                }
                // BR08: below the minimum, say so instead of showing a number
                if (t.reliable) {
                    Text("${t.percent}%  (${t.correct}/${t.answered})", color = accentColor, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        if (t.answered == 0) "Too little data!" else "Too little data! (${t.answered})",
                        color = Muted, fontSize = 12.sp
                    )
                }
            }
        }
        if (footer != null) {
            Spacer(Modifier.height(8.dp))
            footer()
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

/** FR19: a topic's percent-correct across attempts, oldest first, as a connected line chart.
 *  Single series (one topic), so no legend is needed - the line itself stays a neutral brand
 *  hairline (the trend shape is the point, not identity), while each dot carries status color
 *  (the same 70/40 red-amber-green threshold used everywhere else in this app) so performance
 *  reads at a glance. Only the most recent point is directly labeled (never a number on every
 *  point); the Attempt-N/date row beneath doubles as the table-view fallback so the raw values
 *  stay readable even without the chart. */
@Composable
fun ProgressTrendRow(topic: String, points: List<ProgressPoint>) {
    val textMeasurer = rememberTextMeasurer()
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(topic, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().height(100.dp)) {
            val leftPad = 8.dp.toPx(); val rightPad = 8.dp.toPx()
            val topPad = 20.dp.toPx(); val bottomPad = 8.dp.toPx()
            val plotW = size.width - leftPad - rightPad
            val plotH = size.height - topPad - bottomPad

            fun xFor(i: Int) = leftPad + if (points.size <= 1) plotW / 2 else plotW * i / (points.size - 1)
            fun yFor(pct: Int) = topPad + plotH * (1 - pct / 100f)

            // Gridlines at this app's own mastery bands (0/40/70/100), recessive hairlines.
            listOf(0, 40, 70, 100).forEach { pct ->
                val y = yFor(pct)
                drawLine(TrackGray, Offset(leftPad, y), Offset(size.width - rightPad, y), strokeWidth = 1.dp.toPx())
            }

            if (points.size > 1) {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val x = xFor(i); val y = yFor(p.slice.percent)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, Maroon, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            points.forEachIndexed { i, p ->
                val x = xFor(i); val y = yFor(p.slice.percent)
                val pct = p.slice.percent
                val color = if (pct >= 70) CorrectGreen else if (pct >= 40) FlagAmber else WrongRed
                drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(x, y)) // surface ring
                drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y))

                if (i == points.lastIndex) {
                    val style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val measured = textMeasurer.measure("$pct%", style)
                    drawText(
                        textMeasurer, "$pct%", style = style,
                        topLeft = Offset(
                            (x - measured.size.width).coerceAtLeast(leftPad),
                            (y - measured.size.height - 4.dp.toPx()).coerceAtLeast(0f)
                        )
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEachIndexed { i, p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Attempt ${i + 1}", fontSize = 10.sp, color = Muted)
                    Text(dateText(p.submittedAt), fontSize = 10.sp, color = Muted)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- answer rows

@Composable
fun Bubble(
    letter: Char,
    filled: Boolean,
    fill: Color = SelectBlue,
    onClick: (() -> Unit)? = null
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
