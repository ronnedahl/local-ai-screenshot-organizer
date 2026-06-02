package dev.peterbot.skarmkoll.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.peterbot.skarmkoll.data.ScreenshotRepository
import dev.peterbot.skarmkoll.domain.Screenshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Det UI:t observerar. UI-lagret är dumt och renderar bara det här. */
data class ListUiState(
    val isSyncing: Boolean = false,
    val screenshots: List<Screenshot> = emptyList(),
    val newlyAdded: Int = 0
)

/**
 * ViewModel för listskärmen (MVVM). Observerar Room-strömmen och triggar synk mot
 * MediaStore. State exponeras som StateFlow; alla muterande anrop sker i viewModelScope.
 */
@HiltViewModel
class ScreenshotListViewModel @Inject constructor(
    private val repository: ScreenshotRepository
) : ViewModel() {

    private val isSyncing = MutableStateFlow(false)
    private val newlyAdded = MutableStateFlow(0)

    val uiState: StateFlow<ListUiState> =
        combine(
            repository.observeActive(),
            isSyncing,
            newlyAdded
        ) { screenshots, syncing, added ->
            ListUiState(isSyncing = syncing, screenshots = screenshots, newlyAdded = added)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListUiState(isSyncing = true)
        )

    /**
     * Synkar nya skärmdumpar från MediaStore till Room. Anropas när behörighet finns.
     * OCR triggas inte här ännu — det kommer i 1.3 (WorkManager-batch).
     */
    fun sync() {
        viewModelScope.launch {
            isSyncing.value = true
            try {
                val result = repository.sync()
                newlyAdded.value = result.added
            } finally {
                isSyncing.value = false
            }
        }
    }
}
