package com.oneui.sms

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oneui.sms.ui.navigation.OneMessagesNavHost
import com.oneui.sms.ui.theme.OneMessagesTheme

class MainActivity : ComponentActivity() {

    private fun isDefaultSmsApp(): Boolean =
        Telephony.Sms.getDefaultSmsPackage(this) == packageName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneMessagesTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var isDefault by remember { mutableStateOf(isDefaultSmsApp()) }

                    val roleLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult(),
                    ) { isDefault = isDefaultSmsApp() }

                    if (isDefault) {
                        OneMessagesNavHost()
                    } else {
                        DefaultSmsAppPrompt(onRequest = { requestDefaultSmsRole(roleLauncher) })
                    }
                }
            }
        }
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
private fun DefaultSmsAppPrompt(onRequest: () -> Unit) {
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
    }
}
