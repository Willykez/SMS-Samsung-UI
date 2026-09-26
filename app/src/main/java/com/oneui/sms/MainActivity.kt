package com.oneui.sms

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneui.sms.ui.navigation.OneMessagesNavHost
import com.oneui.sms.data.SettingsRepository
import androidx.compose.runtime.collectAsState
import com.oneui.sms.ui.theme.OneMessagesTheme

private const val TAG = "DefaultSmsCheck"

class MainActivity : ComponentActivity() {

    // Held at the Activity level (not inside a remember{} in setContent) so
    // onResume() can push a fresh value into the same state the UI reads.
    private val isDefaultSmsState = mutableStateOf(false)
    // Shown on-screen (not just logcat) so this is debuggable from a screenshot alone.
    private val debugInfoState = mutableStateOf("")
    private val permissionsReadyState = mutableStateOf(false)

    private val requiredPermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()

    private fun hasRequiredPermissions(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun isDefaultSmsApp(): Boolean {
        val currentDefault = Telephony.Sms.getDefaultSmsPackage(this)
        val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(Context.ROLE_SERVICE) as RoleManager
        } else null
        val roleAvailable = roleManager?.isRoleAvailable(RoleManager.ROLE_SMS)
        val roleHeld = roleManager?.isRoleHeld(RoleManager.ROLE_SMS)

        // RoleManager is the actual source of truth on API 29+; the legacy
        // Telephony.Sms.getDefaultSmsPackage() check can return null/stale
        // even after the role is genuinely granted (seen on some devices/
        // emulators with no active telephony subscription). Prefer the role
        // check when it's available, and only fall back to the legacy
        // package-name comparison pre-Q where RoleManager doesn't exist.
        val matches = roleHeld ?: (currentDefault == packageName)

        val info = "our package: $packageName\n" +
            "system default: $currentDefault\n" +
            "match: $matches\n" +
            "ROLE_SMS available: $roleAvailable, held: $roleHeld"
        Log.d(TAG, info.replace("\n", " | "))
        debugInfoState.value = info
        return matches
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        isDefaultSmsState.value = isDefaultSmsApp()
        permissionsReadyState.value = hasRequiredPermissions()

        setContent {
            val settings = SettingsRepository(applicationContext).observe().collectAsState(initial = com.oneui.sms.data.local.SettingsEntity())
            OneMessagesTheme(themeMode = settings.value.themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isDefault by isDefaultSmsState
                    val debugInfo by debugInfoState

                    val roleLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult(),
                    ) {
                        isDefaultSmsState.value = isDefaultSmsApp()
                        permissionsReadyState.value = hasRequiredPermissions()
                    }
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions(),
                    ) { permissionsReadyState.value = hasRequiredPermissions() }

                    if (!isDefault) {
                        DefaultSmsAppPrompt(
                            debugInfo = debugInfo,
                            onRequest = { requestDefaultSmsRole(roleLauncher) },
                            onCheckAgain = { isDefaultSmsState.value = isDefaultSmsApp() },
                        )
                    } else if (!permissionsReadyState.value) {
                        PermissionsPrompt(
                            onRequest = { permissionLauncher.launch(requiredPermissions) },
                            onCheckAgain = { permissionsReadyState.value = hasRequiredPermissions() },
                        )
                    } else {
                        OneMessagesNavHost()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDefaultSmsState.value = isDefaultSmsApp()
        permissionsReadyState.value = hasRequiredPermissions()
    }

    private fun requestDefaultSmsRole(
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    ) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            @Suppress("DEPRECATION")
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
        }
        launcher.launch(intent)
    }
}

@androidx.compose.runtime.Composable
private fun DefaultSmsAppPrompt(debugInfo: String, onRequest: () -> Unit, onCheckAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "To send and receive SMS, this needs to be set as your default messaging app.",
            style = MaterialTheme.typography.titleMedium,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(12.dp))
        Button(onClick = onRequest) {
            Text("Set as default SMS app")
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
        OutlinedButton(onClick = onCheckAgain) {
            Text("Already set it — check again")
        }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(20.dp))
        // Temporary on-screen diagnostics — remove once this is confirmed working.
        Text(debugInfo, style = MaterialTheme.typography.labelSmall)
    }
}


@androidx.compose.runtime.Composable
private fun PermissionsPrompt(onRequest: () -> Unit, onCheckAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Allow access to your SMS history and contacts", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.padding(8.dp))
        Text(
            "OneMessages reads the SMS database already stored on this phone and the local Contacts provider so it can show conversation names and contact photos. Nothing is uploaded to a server.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.padding(16.dp))
        Button(onClick = onRequest) { Text("Allow access") }
        Spacer(Modifier.padding(6.dp))
        OutlinedButton(onClick = onCheckAgain) { Text("Check again") }
    }
}
