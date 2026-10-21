package com.example.main.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.main.R
import com.example.main.statemgmt.AppViewModel

/**
 * LEARNING POINT: Login Screen
 * Allows students to sign in, register, or reset password with Firebase Auth.
 * Errors and success feedback are communicated cleanly via Snackbar.
 * Fully responsive for Portrait and Landscape.
 */
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val emptyEmailMsg = stringResource(R.string.error_empty_email)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.login_email_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { viewModel.signIn(email, password) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.login_btn_signin))
                }

                OutlinedButton(
                    onClick = { viewModel.signUp(email, password) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.login_btn_register))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    if (email.isBlank()) {
                        viewModel.showSnackbar(emptyEmailMsg)
                    } else {
                        viewModel.sendPasswordReset(email) { _, msg ->
                            viewModel.showSnackbar(msg ?: "")
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.login_forgot_password))
            }
        }
    }
}
