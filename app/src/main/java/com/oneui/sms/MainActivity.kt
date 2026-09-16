package com.oneui.sms

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.oneui.sms.ui.theme.OneMessagesTheme

private const val TAG = "DefaultSmsCheck"

class MainActivity : ComponentActivity() {

    // Held at the Activity level (not inside a remember{} in setContent) so
    // onResume() can push a fresh value into the same state the UI reads.
    private val isDefaultSmsState = mutableStateOf(false)
    // Shown on-screen (not just logcat) so this is debuggable from a screenshot alone.
    private val debugInfoState = mutableStateOf("")

    private fun isDefaultSmsApp(): Boolean {
        val currentDefault = Telephony.Sms.getDefaultSmsPackage(this)
        val matches = currentDefault == packageName
        val roleAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (getSystemService(Context.ROLE_SERVICE) as RoleManager).isRoleAvailable(RoleManager.ROLE_SMS)
        } else null
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (getSystemService(Context.ROLE_SERVICE) as RoleManager).isRoleHeld(RoleManager.ROLE_SMS)
        } else null
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
        isDefaultSmsState.value = isDefaultSmsApp()

        setContent {
            OneMessagesTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val isDefault by isDefaultSmsState
                    val debugInfo by debugInfoState

                    val roleLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult(),
                    ) { isDefaultSmsState.value = isDefaultSmsApp() }

                    if (isDefault) {
                        OneMessagesNavHost()
                    } else {
                        DefaultSmsAppPrompt(
                            debugInfo = debugInfo,
                            onRequest = { requestDefaultSmsRole(roleLauncher) },
                            onCheckAgain = { isDefaultSmsState.value = isDefaultSmsApp() },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isDefaultSmsState.value = isDefaultSmsApp()
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
