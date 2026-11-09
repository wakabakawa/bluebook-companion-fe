package com.achievo.bluebook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.SampleData
import com.achievo.bluebook.data.Sheet
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*

/** FR05: set > subject > branch. Unpublished sheets are disabled with a reason. */
@Composable
fun SetsScreen(vm: AppViewModel, onOpenSheet: (String) -> Unit) {
    var selectedSet by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var ascending by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Sheet?>(null) }

    BackHandler(enabled = selectedSet != null) { selectedSet = null }

    fun begin(s: Sheet) {
        vm.startSheet(s)
        onOpenSheet(s.id)
    }
    fun tryStart(s: Sheet) {
        val d = vm.draft
        when {
            d != null && d.sheetId == s.id -> onOpenSheet(s.id)   // resume the same sheet
            d != null -> pending = s                               // would replace another unfinished sheet
            else -> begin(s)
        }
    }

    pending?.let { p ->
        NoticeDialog(
            title = "UNFINISHED SHEET",
            body = "You have an unfinished answer sheet. Starting a new one will discard it.\n\nContinue?",
            confirm = "Start New", onConfirm = { pending = null; begin(p) },
            dismiss = "Back", onDismiss = { pending = null }
        )
    }

    val back: (() -> Unit)? = if (selectedSet != null) ({ selectedSet = null }) else null

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = back)
        val set = selectedSet
        if (set == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SampleData.setNames.forEach { name ->
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Maroon)
                            .clickable { selectedSet = name; expanded = null; query = "" }.padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(name.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
            }
        } else {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("Search") },
                    trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(onClick = { ascending = !ascending }) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = "Sort", tint = Maroon)
                }
            }
            val grouped = SampleData.sheets.filter { it.setName == set }.groupBy { it.subject }
                .filter { (subject, list) ->
                    query.isBlank() || subject.contains(query, true) || list.any { it.branch.contains(query, true) }
                }
                .entries.sortedBy { it.key }.let { if (ascending) it else it.reversed() }

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (grouped.isEmpty()) {
                    item { Text("No exam sets match your search.", color = Muted, modifier = Modifier.padding(8.dp)) }
                }
                items(grouped) { entry ->
                    val isOpen = expanded == entry.key
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Maroon)
                                .clickable { expanded = if (isOpen) null else entry.key }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.key.uppercase(), Modifier.weight(1f), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Color.White)
                        }
                        if (isOpen) entry.value.forEach { s -> BranchRow(s) { tryStart(s) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun BranchRow(s: Sheet, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).alpha(if (s.published) 1f else 0.55f)
            .shadow(2.dp, shape).clip(shape).background(CardPink)
            .clickable(enabled = s.published, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.branch, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${s.itemCount} items  •  ${mmss(s.allotmentSeconds)} mins", fontSize = 12.sp, color = Muted)
            if (!s.published) Text("No published answer key yet", fontSize = 12.sp, color = FlagAmber)
        }
        if (s.published) Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Maroon)
    }
}
