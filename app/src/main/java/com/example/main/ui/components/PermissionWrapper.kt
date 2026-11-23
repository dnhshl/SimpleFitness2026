package com.example.main.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.main.statemgmt.AppViewModel

/**
 * REVERSE ENGINEERING LERNEINHEIT: RECHTEMANAGEMENT (CAMERA PERMISSION)
 *
 * Diese Komponente kapselt die Abfrage der Kamera-Laufzeitberechtigung (`android.permission.CAMERA`).
 *
 * - Wenn die Berechtigung erteilt ist: Rendert die eigentliche App-UI (`content()`).
 * - Wenn die Berechtigung fehlt: Zeigt einen informativen Hinweis-Screen mit Anfrage-Button.
 */
@Composable
fun PermissionWrapper(
    viewModel: AppViewModel,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Zustand, ob die Berechtigung erteilt wurde
    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Informiere das ViewModel über den initialen Status
    LaunchedEffect(isPermissionGranted) {
        viewModel.onCameraPermissionGranted(isPermissionGranted)
    }

    // Compose Launcher für Berechtigungsanfrage
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isPermissionGranted = isGranted
        viewModel.onCameraPermissionGranted(isGranted)
    }

    if (isPermissionGranted) {
        // Berechtigung liegt vor -> Zeige Hauptinhalt (Kamera & Würfel)
        content()
    } else {
        // Berechtigung fehlt -> Zeige Erklärungskarte für Studierende
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = "Kamera Berechtigung",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Kamerazugriff erforderlich",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Für die On-Device AI Gestensteuerung mit MediaPipe wird der Zugriff auf die Frontkamera benötigt. " +
                               "Deine Videodaten verbleiben zu 100% lokal auf dem Gerät.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { launcher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kamera freigeben")
                    }
                }
            }
        }
    }
}
