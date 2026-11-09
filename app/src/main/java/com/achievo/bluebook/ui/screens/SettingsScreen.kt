package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.Profile
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*

private const val TERMS_PLACEHOLDER =
    "Terms and conditions text pending from Achievo Institute. " +
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. (placeholder)"

/** FR24, FR25: profile, privacy notice, terms, clear local data. */
@Composable
fun SettingsScreen(vm: AppViewModel) {
    var editing by remember { mutableStateOf(false) }
    var nick by remember { mutableStateOf(vm.profile.nickname) }
    var school by remember { mutableStateOf(vm.profile.school) }
    var course by remember { mutableStateOf(vm.profile.course) }
    var dialog by remember { mutableStateOf<String?>(null) }

    when (dialog) {
        "privacy" -> NoticeDialog(
            "PRIVACY NOTICE",
            "ALL submissions are anonymous, and data will be handled in compliance with the Data Privacy Act of 2012!",
            "Close", { dialog = null }
        )
        "terms" -> NoticeDialog("TERMS & CONDITIONS", TERMS_PLACEHOLDER, "Close", { dialog = null })
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
            TextButton(onClick = { dialog = "terms" }) { Text("Terms and Conditions", fontSize = 18.sp, color = SelectBlue) }
            TextButton(onClick = { dialog = "privacy" }) { Text("Privacy Notice", fontSize = 18.sp, color = SelectBlue) }
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
