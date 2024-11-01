package com.example.bananadiseaseclassifier.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bananadiseaseclassifier.R
import com.example.bananadiseaseclassifier.robotoCondensedSemibold
import com.example.bananadiseaseclassifier.data.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    val backgroundColor = Color(0xFFF8F5D0)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val passwordsDoNotMatch = stringResource(R.string.passwords_do_not_match)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo_001),
            contentDescription = stringResource(R.string.app_logo_description),
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 40.sp,
            fontFamily = robotoCondensedSemibold,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.confirm_password)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                if (password != confirmPassword) {
                    errorMessage = passwordsDoNotMatch
                } else {
                    coroutineScope.launch {
                        val result = authRepository.signUp(email, password)
                        result.fold(
                            onSuccess = {
                                onRegisterSuccess()
                            },
                            onFailure = { e ->
                                errorMessage = e.message
                            }
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text(stringResource(R.string.register_button), fontSize = 18.sp)
        }

        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onLoginClick) {
            Text(stringResource(R.string.already_have_account))
        }
    }
}