package dev.peterbot.skarmkoll.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.peterbot.skarmkoll.data.ScreenshotRepository
import dev.peterbot.skarmkoll.domain.Screenshot
import dev.peterbot.skarmkoll.work.OcrScheduler
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
    private val repository: ScreenshotRepository,
    private val ocrScheduler: OcrScheduler
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
     * Synkar nya skärmdumpar från MediaStore till Room och köar därefter OCR-batchen.
     * Vi schemalägger alltid OCR efter synk: även om inget nytt tillkom kan det finnas
     * rader som blev kvar obearbetade från en tidigare avbruten körning.
     */
    fun sync() {
        viewModelScope.launch {
            isSyncing.value = true
            try {
                val result = repository.sync()
                newlyAdded.value = result.added
                ocrScheduler.schedule()
            } finally {
                isSyncing.value = false
            }
        }
    }
}
