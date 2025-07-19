package com.example.myapplication



import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Premium {

    private val db = Firebase.firestore

    fun storePremiumUser(email: String, onComplete: (Boolean) -> Unit = {}) {
        val userMap = hashMapOf(
            "email" to email,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("premium_users").document(email).set(userMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun checkIfPremium(email: String, callback: (Boolean) -> Unit) {
        db.collection("premium_users").document(email).get()
            .addOnSuccessListener { doc ->
                callback(doc.exists())
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}
