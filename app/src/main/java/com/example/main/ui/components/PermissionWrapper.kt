package com.example.main.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * PermissionWrapper handles the runtime permissions required for BLE.
 * On Android 12+, we need BLUETOOTH_SCAN and BLUETOOTH_CONNECT (thanks to neverForLocation flag).
 * On older versions, we need ACCESS_FINE_LOCATION.
 */
@Composable
fun PermissionWrapper(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Define which permissions we need based on the Android version
    // On Android 12+ (API 31+), with neverForLocation flag in Manifest, we only need SCAN and CONNECT.
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // Helper to check current permission status
    fun checkPermissions(): Boolean {
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        Log.d("PermissionWrapper", "checkPermissions result = $granted")
        return granted
    }

    var permissionsGranted by remember { mutableStateOf(checkPermissions()) }

    // Re-check permissions whenever the lifecycle resumes (e.g., returning from system settings/dialog)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("PermissionWrapper", "Lifecycle ON_RESUME triggered, re-checking permissions...")
                permissionsGranted = checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Create a launcher to request permissions
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d("PermissionWrapper", "Permission request results: $results")
        permissionsGranted = results.all { it.value } || checkPermissions()
    }

    if (permissionsGranted) {
        // If granted, show the actual app content
        content()
    } else {
        // If not granted, show a button to request them
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bluetooth permissions are required to communicate with the ESP32.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = {
                    Log.d("PermissionWrapper", "Grant Permissions button clicked. checkPermissions=${checkPermissions()}")
                    if (checkPermissions()) {
                        permissionsGranted = true
                    } else {
                        Log.d("PermissionWrapper", "Launching permission request...")
                        launcher.launch(permissions)
                    }
                }
            ) {
                Text("Grant Permissions")
            }
        }
    }
}
