package dev.peterbot.skarmkoll

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.peterbot.skarmkoll.ui.list.ScreenshotListScreen
import dev.peterbot.skarmkoll.ui.permission.PermissionGate
import dev.peterbot.skarmkoll.ui.theme.SkarmKollTheme

/**
 * Enda aktiviteten. @AndroidEntryPoint krävs för att Compose-skärmarnas @HiltViewModel
 * ska kunna injiceras. Behörighet grindas först — inget visas förrän vi vet att vi får
 * läsa bilderna (eller åtminstone en delmängd av dem).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkarmKollTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate { access, onRequestMore ->
                        ScreenshotListScreen(
                            access = access,
                            onRequestMore = onRequestMore
                        )
                    }
                }
            }
        }
    }
}
