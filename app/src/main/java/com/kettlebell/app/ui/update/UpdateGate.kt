package com.kettlebell.app.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kettlebell.app.ui.WorkoutViewModel

/**
 * Shows an "update available" dialog when a newer GitHub release is found on startup. Downloading
 * hands the APK to the system installer; the OS then asks the user to confirm the install.
 */
@Composable
fun UpdateGate(viewModel: WorkoutViewModel) {
    val update by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val downloading by viewModel.updateDownloading.collectAsStateWithLifecycle()
    val percent by viewModel.updateDownloadPercent.collectAsStateWithLifecycle()

    val release = update ?: return

    AlertDialog(
        onDismissRequest = { if (!downloading) viewModel.dismissUpdate() },
        title = { Text("Update available") },
        text = {
            Column {
                Text(
                    text = "Version ${release.version} is available (you have this older build).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (release.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = release.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Downloading… $percent%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.downloadAndInstallUpdate() },
                enabled = !downloading,
            ) { Text(if (downloading) "Downloading…" else "Download & install") }
        },
        dismissButton = {
            if (!downloading) {
                TextButton(onClick = { viewModel.skipUpdate() }) { Text("Skip") }
            }
        },
    )
}
