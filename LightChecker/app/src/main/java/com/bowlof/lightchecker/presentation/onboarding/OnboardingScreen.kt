package com.bowlof.lightchecker.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bowlof.lightchecker.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(ui.locationHint) {
        val hint = ui.locationHint ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(hint)
        viewModel.consumeLocationHint()
    }

    LaunchedEffect(ui.duplicateMessage) {
        val name = ui.duplicateMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(R.string.onboarding_duplicate, name))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.onboarding_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            ui.catalogError -> {
                Text(
                    stringResource(R.string.onboarding_catalog_error),
                    modifier = Modifier.padding(padding).padding(16.dp),
                )
            }

            ui.catalog.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )
            }

            else -> {
                OnboardingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    ui = ui,
                    onCityIndex = viewModel::selectCity,
                    onQueueIndex = viewModel::selectQueue,
                    onUseLocationAfterPermission = viewModel::tryResolveCityByDeviceLocation,
                    onSave = {
                        val city = ui.catalog.getOrNull(ui.selectedCityIndex)
                        val queue = city?.queues?.getOrNull(ui.selectedQueueIndex)
                        if (city == null || queue == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.onboarding_pick_city_queue),
                                )
                            }
                        } else {
                            viewModel.saveFirstPlace(onFinished)
                        }
                    },
                    isSaving = ui.isSaving,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingContent(
    modifier: Modifier,
    ui: OnboardingUiState,
    onCityIndex: (Int) -> Unit,
    onQueueIndex: (Int) -> Unit,
    onUseLocationAfterPermission: () -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
) {
    val city = ui.catalog.getOrNull(ui.selectedCityIndex) ?: ui.catalog.first()
    var cityExpanded by remember { mutableStateOf(false) }
    var queueExpanded by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) {
            onUseLocationAfterPermission()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyLarge)

        OutlinedButton(
            onClick = {
                locationPermissionLauncher.launch(
                    arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.onboarding_use_location))
        }

        ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { cityExpanded = it }) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                value = city.displayName,
                onValueChange = {},
                label = { Text(stringResource(R.string.onboarding_city)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
            )
            DropdownMenu(
                expanded = cityExpanded,
                onDismissRequest = { cityExpanded = false },
            ) {
                for ((index, c) in ui.catalog.withIndex()) {
                    DropdownMenuItem(
                        text = { Text(c.displayName) },
                        onClick = {
                            onCityIndex(index)
                            cityExpanded = false
                        },
                    )
                }
            }
        }

        val queues = city.queues
        if (queues.isNotEmpty()) {
            ExposedDropdownMenuBox(expanded = queueExpanded, onExpandedChange = { queueExpanded = it }) {
                val q = queues.getOrNull(ui.selectedQueueIndex) ?: queues.first()
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = q.displayName,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.onboarding_queue)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = queueExpanded) },
                )
                DropdownMenu(
                    expanded = queueExpanded,
                    onDismissRequest = { queueExpanded = false },
                ) {
                    for ((index, item) in queues.withIndex()) {
                        DropdownMenuItem(
                            text = { Text(item.displayName) },
                            onClick = {
                                onQueueIndex(index)
                                queueExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Button(
            onClick = onSave,
            enabled = !isSaving && queues.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(stringResource(R.string.onboarding_save))
        }
    }
}
