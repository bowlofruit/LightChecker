package com.bowlof.lightchecker.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bowlof.lightchecker.BuildConfig
import com.bowlof.lightchecker.R
import com.bowlof.lightchecker.domain.model.SavedPlace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onAddPlace: () -> Unit = {},
    onHistory: () -> Unit = {},
    onAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val places by viewModel.places.collectAsStateWithLifecycle()
    var placeToDelete by remember { mutableStateOf<SavedPlace?>(null) }

    placeToDelete?.let { place ->
        AlertDialog(
            onDismissRequest = { placeToDelete = null },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_delete_confirm_body,
                        place.cityDisplayName,
                        place.queueDisplayName,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlace(place.id)
                    placeToDelete = null
                }) {
                    Text(stringResource(R.string.settings_delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { placeToDelete = null }) {
                    Text(stringResource(R.string.settings_delete_confirm_no))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlace) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_add_place))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.settings_places),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            itemsIndexed(places, key = { _, p -> p.id }) { index, place ->
                PlaceRow(
                    place = place,
                    isFirst = index == 0,
                    isLast = index == places.lastIndex,
                    onMoveUp = {
                        if (index > 0) viewModel.swapOrder(place.id, places[index - 1].id)
                    },
                    onMoveDown = {
                        if (index < places.lastIndex) viewModel.swapOrder(place.id, places[index + 1].id)
                    },
                    onPrimary = { viewModel.setWidgetPrimary(place.id) },
                    onToggleNotifications = { viewModel.toggleNotifications(place.id, !place.notificationsEnabled) },
                    onDelete = { placeToDelete = place },
                )
            }
            item {
                TextButton(onClick = onHistory) {
                    Text(stringResource(R.string.settings_history))
                }
            }
            item {
                TextButton(onClick = onAbout) {
                    Text(stringResource(R.string.settings_about))
                }
            }
            item {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaceRow(
    place: SavedPlace,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPrimary: () -> Unit,
    onToggleNotifications: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(place.cityDisplayName) },
        supportingContent = { Text(place.queueDisplayName) },
        leadingContent = {
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.settings_move_up))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.settings_move_down))
                }
            }
        },
        trailingContent = {
            Row {
                IconButton(onClick = onPrimary, enabled = !place.isWidgetPrimary) {
                    Icon(Icons.Default.Star, contentDescription = stringResource(R.string.settings_primary_widget))
                }
                IconButton(onClick = onToggleNotifications) {
                    val icon = if (place.notificationsEnabled) {
                        Icons.Default.Notifications
                    } else {
                        Icons.Default.NotificationsOff
                    }
                    val cd = if (place.notificationsEnabled) {
                        stringResource(R.string.settings_notifications_on)
                    } else {
                        stringResource(R.string.settings_notifications_off)
                    }
                    Icon(icon, contentDescription = cd)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.settings_delete))
                }
            }
        },
    )
}
