package com.arjundubey.app

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

@Composable
fun DonationScreen() {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("1") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Donate To Arjun", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") })
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact") })

        Button(
            onClick = {
                val amt = amount.toIntOrNull()
                if (amt != null && amt > 0 && email.isNotBlank()) {
                    // Mark this as a donation
                    (context as? MainActivity)?.isPremiumPurchase = false
                    startPayment(
                        context = context,
                        amount = amt * 100, // Razorpay expects amount in paise
                        name = name,
                        email = email,
                        contact = contact
                    )
                } else {
                    Toast.makeText(context, "Please enter valid details", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors= ButtonDefaults.buttonColors(MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Text("Donate ₹$amount")

        }

    }
}

fun startPayment(
    context: Context,
    amount: Int,
    name: String,
    email: String,
    contact: String
) {
    val activity = context as? Activity ?: return
    val checkout = Checkout()
    checkout.setKeyID("rzp_live_QulPR0m620bxEF") // Same as your frontend key

    val client = OkHttpClient()
    val json = """
        {
            "amount": $amount
        }
    """.trimIndent()

    val requestBody = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("https://www.arjundubey.com/api/create-razorpay-order") // ✅ your working Next.js API
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            activity.runOnUiThread {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string()
            if (!response.isSuccessful || responseBody == null) {
                activity.runOnUiThread {
                    Toast.makeText(context, "Error: $responseBody", Toast.LENGTH_LONG).show()
                }
                return
            }

            val order = JSONObject(responseBody)
            val options = JSONObject().apply {
                put("name", "$name to Arjun Dubey")
                put("description", "Donation")
                put("currency", "INR")
                put("amount", order.getInt("amount"))
                put("order_id", order.getString("id"))
                put("prefill", JSONObject().apply {
                    put("email", email)
                    put("contact", contact)
                    put("name", name)
                })
            }

            activity.runOnUiThread {
                checkout.open(activity, options)
            }
        }
    })
}
@Composable
fun PremiumScreen(onPremiumActivated: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var contact by remember { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.email?.let {
            email = it
        }
    }

    Button(
        onClick = {
            val amount = 1
            if (email.isNotBlank()) {
                (context as? MainActivity)?.isPremiumPurchase = true
                startPayment(
                    context = context,
                    amount = amount * 100,
                    name = name,
                    email = email,
                    contact = contact
                )
                Premium.storePremiumUser(email) { success ->
                    if (success) {
                        Toast.makeText(context, "Premium Activated!", Toast.LENGTH_SHORT).show()
                        onPremiumActivated()
                    }
                }
            } else {
                Toast.makeText(context, "Email required", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Buy Premium ₹199")
    }

}