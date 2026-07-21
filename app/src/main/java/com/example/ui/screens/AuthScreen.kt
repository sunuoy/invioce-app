package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.InvoiceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var newPasswordState by remember { mutableStateOf("") }

    var isSignUpMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authPrefs = remember { context.getSharedPreferences("invoice_generator_prefs", android.content.Context.MODE_PRIVATE) }

    var rememberMe by remember { mutableStateOf(authPrefs.getBoolean("remember_me", true)) }
    var email by remember { mutableStateOf(if (rememberMe) authPrefs.getString("saved_email", "") ?: "" else "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Aesthetic color definitions
    val gradientColors = listOf(
        Color(0xFF0F172A), // Slate 900
        Color(0xFF1E1B4B), // Indigo 950
        Color(0xFF311042)  // Purple 950
    )

    val neonPink = Color(0xFFF43F5E)
    val neonIndigo = Color(0xFF6366F1)
    val neonBlue = Color(0xFF3B82F6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
            .drawBehind {
                // Background Orbs for Premium Ambient Lighting
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(neonPink.copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.25f),
                        radius = size.maxDimension * 0.5f
                    ),
                    radius = size.maxDimension * 0.5f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.25f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(neonBlue.copy(alpha = 0.12f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f),
                        radius = size.maxDimension * 0.45f
                    ),
                    radius = size.maxDimension * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "⚡ Invoice Pro",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 32.sp
            )
            
            Text(
                text = if (isSignUpMode) "Create your professional billing account" else "Sign in to manage your business invoicing",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Text Fields
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Business Email", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = neonIndigo) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = neonIndigo,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedLabelColor = neonIndigo,
                    cursorColor = neonIndigo,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = neonIndigo) },
                trailingIcon = {
                    val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = neonIndigo,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                    focusedLabelColor = neonIndigo,
                    cursorColor = neonIndigo,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                 shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isSignUpMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = neonIndigo,
                                uncheckedColor = Color.White.copy(alpha = 0.6f),
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = "Remind Me",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = { showForgotPasswordDialog = true },
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = neonPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (isSignUpMode) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = neonIndigo) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonIndigo,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = neonIndigo,
                        cursorColor = neonIndigo,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isSignUpMode && password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        val success = if (isSignUpMode) {
                            viewModel.registerUser(email.trim(), password)
                        } else {
                            viewModel.loginUser(email.trim(), password)
                        }

                        if (success) {
                            authPrefs.edit()
                                .putBoolean("remember_me", rememberMe)
                                .putString("saved_email", if (rememberMe) email.trim() else "")
                                .apply()
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = neonIndigo),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isSignUpMode) "Sign Up" else "Sign In",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Toggle Mode Link
            Row(
                modifier = Modifier.clickable {
                    isSignUpMode = !isSignUpMode
                    // Reset field entries
                    email = ""
                    password = ""
                    confirmPassword = ""
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = if (isSignUpMode) "Sign In" else "Sign Up",
                    color = neonPink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showForgotPasswordDialog = false
                    forgotEmail = ""
                    newPasswordState = ""
                },
                title = {
                    Text("Forgot Password", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Enter your registered business email address to receive a secure password reset link in your email inbox.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = { Text("Registered Email") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (forgotEmail.isBlank()) {
                                Toast.makeText(context, "Please enter your registered email", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                val success = viewModel.sendPasswordResetEmail(forgotEmail.trim())
                                if (success) {
                                    showForgotPasswordDialog = false
                                    forgotEmail = ""
                                }
                            }
                        }
                    ) {
                        Text("Send Reset Link")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showForgotPasswordDialog = false
                            forgotEmail = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
