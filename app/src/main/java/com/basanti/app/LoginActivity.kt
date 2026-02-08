package com.basanti.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            val context = LocalContext.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            var loginMode by remember { mutableStateOf("CHOOSE") } // CHOOSE, PHONE, EMAIL, SIGNUP
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            // Sign Up Fields
            var firstName by remember { mutableStateOf("") }
            var lastName by remember { mutableStateOf("") }
            var mobileNumber by remember { mutableStateOf("") }
            var signUpEmail by remember { mutableStateOf("") }
            var pin by remember { mutableStateOf("") }
            var confirmPin by remember { mutableStateOf("") }

            var phoneNumber by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var showExitDialog by remember { mutableStateOf(false) }

            fun resetAllFields() {
                email = ""
                password = ""
                firstName = ""
                lastName = ""
                mobileNumber = ""
                signUpEmail = ""
                pin = ""
                confirmPin = ""
                phoneNumber = ""
                isLoading = false
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Exit App") },
                    text = { Text("Do you want to exit the app?") },
                    confirmButton = {
                        TextButton(onClick = { (context as? Activity)?.finish() }) {
                            Text("Exit")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            BackHandler(enabled = true) {
                if (loginMode != "CHOOSE") {
                    loginMode = "CHOOSE"
                    resetAllFields()
                } else {
                    showExitDialog = true
                }
            }

            val gradient = Brush.verticalGradient(
                colors = listOf(Color(0xFF6650a4), Color(0xFFD0BCFF))
            )

            Box(modifier = Modifier.fillMaxSize().background(gradient)) {
                // Background/Content Column drawn FIRST
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (loginMode) {
                            "CHOOSE" -> "Welcome to Basanti"
                            "SIGNUP" -> "Create Account"
                            "PHONE" -> "Phone Login"
                            else -> "Email Login"
                        },
                        color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    if (loginMode == "CHOOSE") {
                        Button(onClick = { loginMode = "PHONE" }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp)) {
                            Text("Login with Phone", color = Color(0xFF6650a4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loginMode = "EMAIL" }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp)) {
                            Text("Login with Email", color = Color(0xFF6650a4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { loginMode = "SIGNUP" }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), shape = RoundedCornerShape(28.dp), border = BorderStroke(2.dp, Color.White)) {
                            Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                                when (loginMode) {
                                    "SIGNUP" -> {
                                        OutlinedTextField(value = firstName, onValueChange = { if (it.length <= 9) firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = lastName, onValueChange = { if (it.length <= 9) lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = mobileNumber, onValueChange = { if (it.length <= 10) { mobileNumber = it; if (it.length == 10) focusManager.moveFocus(FocusDirection.Down) } }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = signUpEmail, onValueChange = { signUpEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6) { pin = it; if (it.length == 6) focusManager.moveFocus(FocusDirection.Down) } }, label = { Text("6 Digit PIN") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 6) { confirmPin = it; if (it.length == 6) keyboardController?.hide() } }, label = { Text("Confirm PIN") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }))
                                    }
                                    "EMAIL" -> {
                                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(value = password, onValueChange = { if (it.length <= 6) { password = it; if (it.length == 6) keyboardController?.hide() } }, label = { Text("6 Digit PIN") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }))
                                    }
                                    "PHONE" -> {
                                        OutlinedTextField(value = phoneNumber, onValueChange = { if (it.length <= 10) { phoneNumber = it; if (it.length == 10) focusManager.moveFocus(FocusDirection.Down) } }, label = { Text("Phone Number") }, prefix = { Text("+91 ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(value = password, onValueChange = { if (it.length <= 6) { password = it; if (it.length == 6) keyboardController?.hide() } }, label = { Text("6 Digit PIN") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }))
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                if (isLoading) {
                                    CircularProgressIndicator(color = Color(0xFF6650a4))
                                } else {
                                    Button(
                                        onClick = {
                                            when (loginMode) {
                                                "SIGNUP" -> {
                                                    if (firstName.isEmpty() || lastName.isEmpty() || mobileNumber.isEmpty() || signUpEmail.isEmpty() || pin.isEmpty()) {
                                                        Toast.makeText(this@LoginActivity, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                                    } else if (!signUpEmail.contains("@")) {
                                                        Toast.makeText(this@LoginActivity, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                                                    } else if (pin != confirmPin) {
                                                        Toast.makeText(this@LoginActivity, "PINs do not match", Toast.LENGTH_SHORT).show()
                                                    } else if (pin.length < 6) {
                                                        Toast.makeText(this@LoginActivity, "PIN must be 6 digits", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        isLoading = true
                                                        auth.createUserWithEmailAndPassword(signUpEmail, pin).addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                val uid = auth.currentUser?.uid ?: ""
                                                                val userMap = hashMapOf(
                                                                    "uid" to uid,
                                                                    "firstName" to firstName,
                                                                    "lastName" to lastName,
                                                                    "phoneNumber" to "+91$mobileNumber",
                                                                    "email" to signUpEmail,
                                                                    "createdAt" to Timestamp.now()
                                                                )
                                                                FirebaseFirestore.getInstance().collection("users").document(uid).set(userMap)
                                                                    .addOnSuccessListener {
                                                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                                                        finish()
                                                                    }
                                                            } else {
                                                                isLoading = false
                                                                Toast.makeText(this@LoginActivity, task.exception?.message, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                                "EMAIL" -> {
                                                    if (email.isNotEmpty() && password.isNotEmpty()) {
                                                        isLoading = true
                                                        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                                                finish()
                                                            } else {
                                                                isLoading = false
                                                                Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                                "PHONE" -> {
                                                    if (phoneNumber.length == 10 && password.length == 6) {
                                                        isLoading = true
                                                        val fullPhone = "+91$phoneNumber"
                                                        FirebaseFirestore.getInstance().collection("users")
                                                            .whereEqualTo("phoneNumber", fullPhone)
                                                            .get()
                                                            .addOnSuccessListener { snapshot ->
                                                                if (!snapshot.isEmpty) {
                                                                    val userEmail = snapshot.documents[0].getString("email") ?: ""
                                                                    if (userEmail.isNotEmpty()) {
                                                                        auth.signInWithEmailAndPassword(userEmail, password)
                                                                            .addOnCompleteListener { task ->
                                                                                if (task.isSuccessful) {
                                                                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                                                                    finish()
                                                                                } else {
                                                                                    isLoading = false
                                                                                    Toast.makeText(this@LoginActivity, "Login Failed: Invalid PIN", Toast.LENGTH_SHORT).show()
                                                                                }
                                                                            }
                                                                    } else {
                                                                        isLoading = false
                                                                        Toast.makeText(this@LoginActivity, "Error: No email linked to this phone", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                } else {
                                                                    isLoading = false
                                                                    Toast.makeText(this@LoginActivity, "User not found. Please Sign Up first.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                            .addOnFailureListener {
                                                                isLoading = false
                                                                Toast.makeText(this@LoginActivity, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                    } else {
                                                        Toast.makeText(this@LoginActivity, "Enter 10-digit Phone and 6-digit PIN", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6650a4)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Submit", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Back arrow drawn LAST to be on top of everything
                if (loginMode != "CHOOSE") {
                    IconButton(
                        onClick = { 
                            loginMode = "CHOOSE"
                            resetAllFields()
                         },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
