package dev.peterbot.skarmkoll.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.peterbot.skarmkoll.R

/**
 * Grindar resten av appen bakom bildbehörighet.
 *
 * Designval: vi visar en egen FÖRKLARINGSSKÄRM innan systemdialogen — integritet är hela
 * poängen med appen, så användaren ska förstå varför vi vill åt bilderna innan Android
 * frågar. PARTIAL (Android 14:s delvisa åtkomst) släpps igenom till [content]; vi krånglar
 * inte, utan jobbar med de bilder vi fått och visar en diskret banner för att välja fler.
 */
@Composable
fun PermissionGate(
    content: @Composable (access: MediaAccess, onRequestMore: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(MediaPermissions.currentAccess(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        access = MediaPermissions.accessFromResult(result)
    }

    when (access) {
        // Vid partiell åtkomst kan användaren trycka "välj fler" → samma systemdialog igen.
        MediaAccess.FULL, MediaAccess.PARTIAL ->
            content(access) { launcher.launch(MediaPermissions.required) }
        MediaAccess.NONE -> RationaleScreen(
            onRequest = { launcher.launch(MediaPermissions.required) },
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun RationaleScreen(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.perm_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.perm_rationale),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )
        Button(onClick = onRequest) {
            Text(stringResource(R.string.perm_grant))
        }
        TextButton(onClick = onOpenSettings) {
            Text(stringResource(R.string.perm_open_settings))
        }
    }
}
