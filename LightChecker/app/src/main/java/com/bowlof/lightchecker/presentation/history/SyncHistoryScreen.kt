package com.bowlof.lightchecker.presentation.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.domain.model.SyncHistoryRecord
import com.bowlof.lightchecker.presentation.util.EmptyStateBox
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncHistoryRoute(
    onBack: () -> Unit,
    viewModel: SyncHistoryViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back_content_desc),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyStateBox(
                icon = Icons.Outlined.History,
                message = stringResource(R.string.history_empty),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: SyncHistoryRecord) {
    val timeText = DateTimeFormatter.ofPattern("dd.MM HH:mm")
        .format(Instant.ofEpochMilli(entry.syncedAtEpochMillis).atZone(KYIV_ZONE))

    val versionText = if (entry.oldVersion != null) {
        stringResource(R.string.history_version, entry.oldVersion, entry.newVersion)
    } else {
        stringResource(R.string.history_version_first, entry.newVersion)
    }

    ListItem(
        headlineContent = { Text(entry.cityDisplayName.ifEmpty { entry.regionId }) },
        supportingContent = { Text("$versionText · ${entry.dateYyyymmdd}") },
        trailingContent = { Text(timeText) },
    )
}

private val KYIV_ZONE = ZoneId.of("Europe/Kyiv")
