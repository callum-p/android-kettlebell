package com.kettlebell.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kettlebell.app.badges.BadgeState
import com.kettlebell.app.badges.Badges

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesScreen(earnedBadgeIds: Set<String>) {
    val badges = remember(earnedBadgeIds) {
        Badges.all.map { BadgeState(it, it.id in earnedBadgeIds) }
    }
    val earnedCount = badges.count { it.earned }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProgressHeader(earned = earnedCount, total = badges.size)
                Spacer(Modifier.height(4.dp))
            }
            items(badges.chunked(3)) { rowBadges ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowBadges.forEach { badgeState ->
                        BadgeTile(badgeState, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - rowBadges.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ProgressHeader(earned: Int, total: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "$earned of $total unlocked",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (earned == total) "You've earned them all — legend! 🏆" else "Keep training to unlock more.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else earned.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun BadgeTile(state: BadgeState, modifier: Modifier = Modifier) {
    val container = if (state.earned) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.badge.emoji,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.alpha(if (state.earned) 1f else 0.35f),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.badge.title,
                style = MaterialTheme.typography.labelLarge,
                color = if (state.earned) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state.earned) "Earned ✓" else state.badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
        }
    }
}
