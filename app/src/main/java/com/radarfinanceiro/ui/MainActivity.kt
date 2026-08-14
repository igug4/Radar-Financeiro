package com.radarfinanceiro.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radarfinanceiro.service.TransactionNotificationListener
import com.radarfinanceiro.ui.screens.MainScreen
import com.radarfinanceiro.ui.screens.MainViewModel
import com.radarfinanceiro.ui.theme.RadarFinanceiroTheme

class MainActivity : ComponentActivity() {

    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Permissoes concedidas ou nao, o app funciona mesmo sem algumas
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicita permissoes
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }

        setContent {
            RadarFinanceiroTheme {
                val viewModel: MainViewModel = viewModel()
                var showSetup by remember { mutableStateOf(!isNotificationListenerEnabled()) }

                if (showSetup) {
                    SetupScreen(
                        notificationListenerEnabled = isNotificationListenerEnabled(),
                        overlayEnabled = Settings.canDrawOverlays(this),
                        onEnableNotificationListener = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onEnableOverlay = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        },
                        onContinue = { showSetup = false }
                    )
                } else {
                    MainScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verifica se as permissoes foram concedidas ao voltar das configuracoes
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, TransactionNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(cn.flattenToString()) == true
    }
}

@Composable
fun SetupScreen(
    notificationListenerEnabled: Boolean,
    overlayEnabled: Boolean,
    onEnableNotificationListener: () -> Unit,
    onEnableOverlay: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Configuracao inicial",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "O Radar Financeiro precisa de algumas permissoes para funcionar corretamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permissao 1: Notification Listener
        SetupItem(
            title = "1. Acesso as notificacoes",
            description = "Permite capturar compras do Itau e Carteira do Google automaticamente.",
            enabled = notificationListenerEnabled,
            onAction = onEnableNotificationListener
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Permissao 2: Overlay
        SetupItem(
            title = "2. Sobrepor outros apps",
            description = "Permite mostrar o pop-up de anotacao quando uma compra e detectada.",
            enabled = overlayEnabled,
            onAction = onEnableOverlay
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = notificationListenerEnabled && overlayEnabled
        ) {
            Text("Comecar a usar")
        }

        if (!notificationListenerEnabled || !overlayEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onContinue) {
                Text("Pular (configurar depois)")
            }
        }
    }
}

@Composable
fun SetupItem(
    title: String,
    description: String,
    enabled: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (enabled) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            } else {
                OutlinedButton(onClick = onAction) {
                    Text("Ativar")
                }
            }
        }
    }
}
