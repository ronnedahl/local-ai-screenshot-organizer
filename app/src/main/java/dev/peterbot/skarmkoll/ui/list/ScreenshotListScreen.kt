package dev.peterbot.skarmkoll.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.peterbot.skarmkoll.R
import dev.peterbot.skarmkoll.domain.Screenshot
import dev.peterbot.skarmkoll.ui.permission.MediaAccess

/**
 * Listskärm (rutnät) över synkade skärmdumpar. Dum vy: observerar [ListUiState] och
 * triggar synk en gång när skärmen visas (och igen om åtkomsten ändras, t.ex. efter
 * att fler bilder valts vid partiell åtkomst).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotListScreen(
    access: MediaAccess,
    onRequestMore: () -> Unit,
    viewModel: ScreenshotListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(access) { viewModel.sync() }

    val processed = state.screenshots.count { it.processed }
    val total = state.screenshots.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.list_title))
                        // Visa OCR-framsteg bara medan något återstår — försvinner när allt är klart.
                        if (total > 0 && processed < total) {
                            Text(
                                text = stringResource(R.string.list_ocr_progress, processed, total),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (access == MediaAccess.PARTIAL) {
                PartialAccessBanner(onSelectMore = onRequestMore)
            }
            when {
                state.isSyncing && state.screenshots.isEmpty() -> LoadingState()
                state.screenshots.isEmpty() -> EmptyState()
                else -> ScreenshotGrid(state.screenshots)
            }
        }
    }
}

@Composable
private fun ScreenshotGrid(items: List<Screenshot>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { screenshot ->
            Box {
                AsyncImage(
                    model = screenshot.uri,
                    contentDescription = stringResource(R.string.cd_thumbnail),
                    modifier = Modifier.aspectRatio(0.6f)
                )
                // Badge medan bilden väntar på OCR. När workern skrivit ocrText
                // emitterar Room-strömmen om och badgen försvinner live.
                if (!screenshot.processed) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.badge_unprocessed),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartialAccessBanner(onSelectMore: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.perm_partial_banner),
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onSelectMore) {
                Text(stringResource(R.string.perm_partial_action))
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.list_loading),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.list_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.list_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
