package com.arjundubey.app

import android.app.Activity
import android.content.Context

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import androidx.compose.ui.platform.LocalLifecycleOwner


import com.google.firebase.auth.FirebaseUser

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Logging
import android.util.Log

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit = {} // Make it optional with default empty implementation
) {
    val context = LocalContext.current
    val activity = context as Activity

    val firestore = remember { FirebaseFirestore.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var userEmail by remember { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id)) // from google-services.json
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val auth = Firebase.auth
    var user by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        user = auth.currentUser
                    } else {
                        Log.e("Auth", "Firebase sign in failed", task.exception)
                    }
                }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Google sign-in failed", e)
        }
    }

    // Check if user is already logged in
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            isLoggedIn = true
            userEmail = currentUser.email ?: ""
        }
    }

    // If user is logged in, show the logout screen
    if (isLoggedIn) {
        LoggedInScreen(
            userEmail = userEmail,
            onLogout = {
                loading = true
                auth.signOut()
                // Also sign out from Google
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("101881111630-i168pccki611htojqfbqq9rmje00iecm.apps.googleusercontent.com")
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(activity, gso)
                client.signOut().addOnCompleteListener {
                    loading = false
                    isLoggedIn = false
                    userEmail = ""
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                }
            }
        )
        return
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLoginMode) "Login" else "Register",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onBackground // or any color you want
            )
        )


        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                loading = true
                if (isLoginMode) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                isLoggedIn = true
                                userEmail = auth.currentUser?.email ?: ""
                                Toast.makeText(context, "Login Success", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Login Failed: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                saveUserToFirestore(
                                    account = null,
                                    firestore = firestore,
                                    context = context,
                                    name = user?.email?.substringBefore("@"),
                                    email = user?.email
                                ) {
                                    isLoggedIn = true
                                    userEmail = user?.email ?: ""
                                    onLoginSuccess()
                                }
                                Toast.makeText(context, "Registered Successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Registration Failed: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Text(if (isLoginMode) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (user == null) {
            Button(onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }) {
                Text("Sign in with Google")
            }
        }
        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "Don't have an account? Register" else "Already have an account? Login")
        }
        if (loading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun LoggedInScreen(
    userEmail: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Logged in as:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = userEmail,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
        ) {
            Text("Logout")
        }
    }
}

fun saveUserToFirestore(
    account: GoogleSignInAccount?,
    firestore: FirebaseFirestore,
    context: Context,
    name: String? = null,
    email: String? = null,
    onComplete: (() -> Unit)? = null
) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val userDoc = firestore.collection("users").document(user.uid)

    userDoc.get().addOnSuccessListener { doc ->
        if (!doc.exists()) {
            val data = mapOf(
                "name" to (account?.displayName ?: name),
                "email" to (account?.email ?: email),
                "createdAt" to FieldValue.serverTimestamp()
            )
            userDoc.set(data)
                .addOnSuccessListener {
                    onComplete?.invoke()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to save user: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            onComplete?.invoke()
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Failed to check user: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}