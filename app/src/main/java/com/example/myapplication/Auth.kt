package com.example.myapplication



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
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AuthScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        saveUserToFirestore(account, firestore, context)
                        Toast.makeText(context, "Google Sign-In Success", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Google Sign-In error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
            style = MaterialTheme.typography.headlineMedium
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
                                Toast.makeText(context, "Login Success", Toast.LENGTH_SHORT).show()
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
                                )
                                Toast.makeText(context, "Registered Successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Registration Failed: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {
            Text(if (isLoginMode) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("101881111630-i168pccki611htojqfbqq9rmje00iecm.apps.googleusercontent.com") // <-- Replace this
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(activity, gso)
                launcher.launch(client.signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {
            Text("Continue with Google")
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

fun saveUserToFirestore(
    account: GoogleSignInAccount?,
    firestore: FirebaseFirestore,
    context: Context,
    name: String? = null,
    email: String? = null
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
            userDoc.set(data).addOnFailureListener {
                Toast.makeText(context, "Failed to save user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
