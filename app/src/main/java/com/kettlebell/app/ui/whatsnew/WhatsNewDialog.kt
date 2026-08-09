package com.kettlebell.app.ui.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Shows the "What's new" modal once after an update. Records the current version whether or not
 * the modal is shown, so a fresh install sets the baseline and the next update triggers the modal.
 */
@Composable
fun WhatsNewGate() {
    val context = LocalContext.current
    // Decide once per composition lifetime; capturing here avoids re-showing after markSeen.
    var visible by remember { mutableStateOf(WhatsNew.shouldShow(context)) }

    LaunchedEffect(Unit) {
        if (!visible) {
            // Fresh install (or nothing to show): quietly record the baseline version.
            WhatsNew.markSeen(context)
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            WhatsNew.markSeen(context)
            visible = false
        },
        title = { Text("What's new ✨") },
        text = {
            Column {
                WhatsNew.entries.forEach { entry ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                WhatsNew.markSeen(context)
                visible = false
            }) { Text("Got it") }
        },
    )
}
