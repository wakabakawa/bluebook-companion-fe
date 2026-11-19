package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.ui.components.BluebookTopBar
import com.achievo.bluebook.ui.components.OutlineButton
import com.achievo.bluebook.ui.components.PrimaryButton
import com.achievo.bluebook.ui.theme.FlagAmber
import com.achievo.bluebook.ui.theme.FlagBg
import com.achievo.bluebook.ui.theme.Maroon
import com.achievo.bluebook.ui.theme.Muted

data class LegalSection(val heading: String?, val body: String)

/** The scrollable title+sections content shared by [LegalScreen] and [PrivacyOnboardingScreen] -
 *  just the body, so each caller can wrap it with whatever chrome (back button, or accept/reject
 *  buttons) fits its own context. */
@Composable
private fun LegalBody(title: String, sections: List<LegalSection>, placeholderNotice: String?, modifier: Modifier = Modifier) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (placeholderNotice != null) {
            Row(
                Modifier.fillMaxWidth().background(FlagBg, RoundedCornerShape(10.dp))
                    .border(1.dp, FlagAmber, RoundedCornerShape(10.dp)).padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = FlagAmber, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(placeholderNotice, fontSize = 13.sp, color = Muted)
            }
        }
        sections.forEach { s ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (s.heading != null) Text(s.heading, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Maroon)
                Text(s.body, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

/** Shared shell for long-form text (Terms, Privacy) - a scrollable, left-aligned page with its
 *  own back button, instead of cramming paragraphs into a small centered AlertDialog. */
@Composable
fun LegalScreen(title: String, sections: List<LegalSection>, placeholderNotice: String?, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        LegalBody(title, sections, placeholderNotice, Modifier.fillMaxSize())
    }
}

private val TERMS_SECTIONS = listOf(
    LegalSection(
        null,
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut " +
            "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
            "laboris nisi ut aliquip ex ea commodo consequat."
    ),
    LegalSection(
        "Use of the App",
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
            "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt " +
            "mollit anim id est laborum."
    ),
    LegalSection(
        "Limitation of Liability",
        "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, " +
            "totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae."
    )
)

private const val TERMS_PLACEHOLDER_NOTICE =
    "This is placeholder text, not final legal copy. Achievo Institute hasn't supplied the app's " +
        "real Terms and Conditions yet - this page exists to show the layout they'll appear in once they do."

/** A professional expansion of the app's existing one-line notice - "ALL submissions are
 *  anonymous, and data will be handled in compliance with the Data Privacy Act of 2012!" -
 *  into its two underlying claims. Not a new policy; just that sentence, explained properly. */
private val PRIVACY_SECTIONS = listOf(
    LegalSection(
        null,
        "This notice explains two commitments this app makes to you: that your submissions are " +
            "anonymous, and that your data is handled in compliance with the Data Privacy Act of 2012 " +
            "(Republic Act No. 10173)."
    ),
    LegalSection(
        "Your Submissions Are Anonymous",
        "Every answer sheet you submit is tied to a randomly generated identifier, not to your name, " +
            "email, or any other personal detail. You are never asked to log in or create an account. " +
            "This identifier exists only so the app can tell your own answers and results apart from " +
            "anyone else's - on its own, it does not reveal who you are."
    ),
    LegalSection(
        "Compliance with the Data Privacy Act of 2012",
        "The Data Privacy Act of 2012 (Republic Act No. 10173) is the Philippine law governing the " +
            "collection and handling of personal data. In line with it, this app:\n" +
            "• Collects only what's needed for the app to function, and nothing more.\n" +
            "• Only processes information you knowingly and voluntarily provide.\n" +
            "• Keeps optional profile details (nickname, school, course) on your device, under your control.\n" +
            "You may exercise your rights under the Act - including to know what is held about you and to " +
            "have it corrected or withdrawn - through the data controls available in Settings."
    )
)

@Composable
fun TermsScreen(onBack: () -> Unit) = LegalScreen("Terms and Conditions", TERMS_SECTIONS, TERMS_PLACEHOLDER_NOTICE, onBack)

@Composable
fun PrivacyScreen(onBack: () -> Unit) = LegalScreen("Privacy Notice", PRIVACY_SECTIONS, null, onBack)

/** FR04's gate, shown full-screen instead of a dialog so the notice gets real reading room.
 *  Same semantics as the dialog it replaces: Accept continues into the app, Reject exits it -
 *  there's no back button, since there's no third option. */
@Composable
fun PrivacyOnboardingScreen(onAccept: () -> Unit, onReject: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BluebookTopBar()
        LegalBody("Privacy Notice", PRIVACY_SECTIONS, null, Modifier.weight(1f).fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton("Reject", onReject, Modifier.weight(1f))
            PrimaryButton("Accept", onAccept, Modifier.weight(1f))
        }
    }
}
