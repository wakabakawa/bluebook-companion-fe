package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.Profile
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*

/** FR24, FR25: profile, privacy notice, terms, clear local data. */
@Composable
fun SettingsScreen(vm: AppViewModel, onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var nick by remember { mutableStateOf(vm.profile.nickname) }
    var school by remember { mutableStateOf(vm.profile.school) }
    var course by remember { mutableStateOf(vm.profile.course) }
    var dialog by remember { mutableStateOf<String?>(null) }

    when (dialog) {
        "clear" -> NoticeDialog(
            title = "CLEAR LOCAL DATA?",
            body = "Your local profile, unfinished sheet and attempt history will be permanently removed and cannot be recovered.\n\n" +
                "Anonymous records already added to the cohort statistics cannot be individually withdrawn, because they carry no identifier linking them to this device.",
            confirm = "Clear",
            onConfirm = {
                vm.clearLocalData()
                nick = ""; school = ""; course = ""; editing = false
                dialog = null
            },
            dismiss = "Cancel", onDismiss = { dialog = null }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BluebookTopBar()
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("User Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    if (editing) { nick = vm.profile.nickname; school = vm.profile.school; course = vm.profile.course }
                    editing = !editing
                }) { Text(if (editing) "CANCEL" else "EDIT", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
            // All optional and skippable (FR02)
            OutlinedTextField(nick, { nick = it }, Modifier.fillMaxWidth(), enabled = editing, singleLine = true, label = { Text("Nickname") })
            OutlinedTextField(school, { school = it }, Modifier.fillMaxWidth(), enabled = editing, singleLine = true, label = { Text("School") })
            OutlinedTextField(course, { course = it }, Modifier.fillMaxWidth(), enabled = editing, singleLine = true, label = { Text("Course") })
            if (editing) PrimaryButton("Save", { vm.saveProfile(Profile(nick.trim(), school.trim(), course.trim())); editing = false })

            Text("About", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            AboutLinkRow("Terms and Conditions", Icons.Outlined.Article, onOpenTerms)
            AboutLinkRow("Privacy Notice", Icons.Outlined.PrivacyTip, onOpenPrivacy)
            // FR25: version and book edition. TODO: edition comes from the backend (CC10).
            Text("App version 0.1.0  •  Book edition: sample data", fontSize = 12.sp, color = Muted)

            Text("Data Management", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            OutlinedButton(onClick = { dialog = "clear" }) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Clear Local Data")
            }
        }
    }
}

@Composable
private fun AboutLinkRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Maroon, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Maroon)
        }
    }
}
