package com.bowlof.lightchecker.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.bowlof.lightchecker.BuildConfig
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.widget.OutageWidgetReceiver
import com.bowlof.lightchecker.domain.model.OutageInterval
import com.bowlof.lightchecker.domain.model.SelectedScheduleDay
import com.bowlof.lightchecker.presentation.util.EmptyStateBox
import com.bowlof.lightchecker.domain.time.KyivTime
import com.bowlof.lightchecker.domain.usecase.NextOutageCalculator
import com.bowlof.lightchecker.domain.usecase.OutageStatus
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

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
            floatingActionButton = {
                val context = LocalContext.current
                FloatingActionButton(
                    onClick = {
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val provider = ComponentName(context, OutageWidgetReceiver::class.java)
                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                            appWidgetManager.requestPinAppWidget(provider, null, null)
                        }
                    },
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = stringResource(R.string.schedule_add_widget))
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (ui.places.size > 1) {
                    var placeDropdownExpanded by remember { mutableStateOf(false) }
                    val safeIndex = ui.selectedTabIndex.coerceIn(0, ui.places.lastIndex)
                    val selectedPlace = ui.places[safeIndex]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = placeDropdownExpanded,
                            onExpandedChange = { placeDropdownExpanded = it },
                            modifier = Modifier.weight(1f),
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = true,
                                value = "${selectedPlace.cityDisplayName} · ${selectedPlace.queueDisplayName}",
                                onValueChange = {},
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = placeDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                singleLine = true,
                            )
                            ExposedDropdownMenu(
                                expanded = placeDropdownExpanded,
                                onDismissRequest = { placeDropdownExpanded = false },
                            ) {
                                ui.places.forEachIndexed { index, place ->
                                    DropdownMenuItem(
                                        text = { Text("${place.cityDisplayName} · ${place.queueDisplayName}") },
                                        onClick = {
                                            viewModel.selectTab(index)
                                            placeDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { viewModel.setWidgetPrimary(selectedPlace.id) },
                            enabled = !selectedPlace.isWidgetPrimary,
                        ) {
                            Icon(
                                Icons.Default.Widgets,
                                contentDescription = stringResource(R.string.schedule_set_widget),
                                tint = if (selectedPlace.isWidgetPrimary) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
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

                if (BuildConfig.DEBUG && ui.places.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.setDemoUiDataEnabled(!ui.isDemoUiData) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text(
                            stringResource(
                                if (ui.isDemoUiData) {
                                    R.string.schedule_hide_demo_data
                                } else {
                                    R.string.schedule_show_demo_data
                                },
                            ),
                        )
                    }
                }

                ui.lastSyncFormatted?.let { syncTime ->
                    Text(
                        stringResource(R.string.schedule_last_sync, syncTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (ui.selectedDay == SelectedScheduleDay.Today && ui.intervalLines.isNotEmpty()) {
                    CountdownBanner(
                        intervals = ui.intervals,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                when {
                    ui.places.isEmpty() -> {
                        EmptyStateBox(
                            icon = Icons.Outlined.AddLocationAlt,
                            message = stringResource(R.string.onboarding_pick_city_queue),
                            actionLabel = stringResource(R.string.settings_add_place),
                            onAction = onOpenSettings,
                            modifier = Modifier.padding(32.dp),
                        )
                    }

                    !ui.hasDataForSelectedDay && ui.intervalLines.isEmpty() -> {
                        EmptyStateBox(
                            icon = Icons.Outlined.EventBusy,
                            message = stringResource(R.string.schedule_empty_day),
                            modifier = Modifier.padding(32.dp),
                        )
                    }

                    ui.intervalLines.isNotEmpty() -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(ui.intervalLines, key = { it }) { line ->
                                ListItem(
                                    headlineContent = { Text(line) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }

                    else -> {
                        EmptyStateBox(
                            icon = Icons.Outlined.LightMode,
                            message = stringResource(R.string.schedule_no_outages),
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownBanner(
    intervals: List<OutageInterval>,
    modifier: Modifier = Modifier,
) {
    // Recompute every 60 seconds
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(tick) {
        delay(60_000)
        tick++
    }

    val now = remember(tick) {
        val kyivNow = ZonedDateTime.now(KyivTime.zone)
        kyivNow.hour * 60 + kyivNow.minute
    }
    val status = remember(intervals, now) {
        NextOutageCalculator.calculate(intervals, now)
    }

    val text = when (status) {
        is OutageStatus.CurrentlyOff -> stringResource(
            R.string.schedule_currently_off,
            NextOutageCalculator.formatMinute(status.endsAtMinute),
        )
        is OutageStatus.NextOff -> {
            val h = status.minutesUntil / 60
            val m = status.minutesUntil % 60
            if (h > 0) {
                stringResource(R.string.schedule_next_off_hours, h, m)
            } else {
                stringResource(R.string.schedule_next_off_minutes, m)
            }
        }
        is OutageStatus.AllDone -> stringResource(R.string.schedule_all_done)
        is OutageStatus.NoData -> return
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                is OutageStatus.CurrentlyOff -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
            color = when (status) {
                is OutageStatus.CurrentlyOff -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            },
        )
    }
}
