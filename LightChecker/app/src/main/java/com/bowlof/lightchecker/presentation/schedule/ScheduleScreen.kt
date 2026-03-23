package com.bowlof.lightchecker.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.domain.model.SelectedScheduleDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRoute(
    onOpenSettings: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { snackbarHostState.showSnackbar(it) }
    }

    val settingsCd = stringResource(R.string.nav_settings_content_desc)

    PullToRefreshBox(
        isRefreshing = ui.isRefreshing,
        onRefresh = { viewModel.refresh() },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.schedule_title)) },
                    actions = {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.semantics {
                                contentDescription = settingsCd
                            },
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (ui.places.size > 1) {
                    PrimaryTabRow(selectedTabIndex = ui.selectedTabIndex.coerceIn(0, ui.places.lastIndex)) {
                        for (index in ui.places.indices) {
                            val place = ui.places[index]
                            val tabCd = stringResource(
                                R.string.schedule_tab_place_cd,
                                place.cityDisplayName,
                                place.queueDisplayName,
                            )
                            Tab(
                                selected = index == ui.selectedTabIndex,
                                onClick = { viewModel.selectTab(index) },
                                text = {
                                    Text(
                                        "${place.cityDisplayName}\n${place.queueDisplayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 2,
                                    )
                                },
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    contentDescription = tabCd
                                },
                            )
                        }
                    }
                }

                val dayOptions = listOf(
                    SelectedScheduleDay.Today to stringResource(R.string.schedule_today),
                    SelectedScheduleDay.Tomorrow to stringResource(R.string.schedule_tomorrow),
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    for (index in dayOptions.indices) {
                        val (day, label) = dayOptions[index]
                        val dayCd = stringResource(R.string.schedule_day_segment_cd, label)
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = dayOptions.size),
                            onClick = { viewModel.selectDay(day) },
                            selected = ui.selectedDay == day,
                            modifier = Modifier.semantics {
                                contentDescription = dayCd
                            },
                        ) {
                            Text(label)
                        }
                    }
                }

                when {
                    ui.places.isEmpty() -> {
                        Text(
                            stringResource(R.string.onboarding_pick_city_queue),
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    !ui.hasDataForSelectedDay && ui.intervalLines.isEmpty() -> {
                        Text(
                            stringResource(R.string.schedule_empty_day),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    ui.intervalLines.isNotEmpty() -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(ui.intervalLines, key = { it }) { line ->
                                ListItem(headlineContent = { Text(line) })
                            }
                        }
                    }

                    else -> {
                        Text(
                            stringResource(R.string.schedule_no_outages),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
