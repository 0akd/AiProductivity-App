package com.arjundubey.app



import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class Note(
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun JournalScreen() {
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var newNote by remember { mutableStateOf("") }

    // Fetch notes when the composable is first launched
    LaunchedEffect(Unit) {
        FirestoreHelper.getNotes { fetchedNotes ->
            notes = fetchedNotes
        }
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = newNote,
            onValueChange = { newNote = it },
            label = { Text("Enter a note") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (newNote.isNotBlank()) {
                    FirestoreHelper.addNote(Note(text = newNote)) { success ->
                        if (success) newNote = ""
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Save Note")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Last 7 Notes", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(notes) { note ->
                Card(modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(note.text)
                        Text(
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(
                                Date(note.timestamp)
                            ),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

object FirestoreHelper {
    private val db = FirebaseFirestore.getInstance()
    private val notesRef = db.collection("notes")

    fun addNote(note: Note, onComplete: (Boolean) -> Unit) {
        notesRef.add(note)
            .addOnSuccessListener {
                deleteOldNotesIfNeeded()
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to add note", e)
                onComplete(false)
            }
    }

    private fun deleteOldNotesIfNeeded() {
        notesRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.size() > 7) {
                    val extraCount = docs.size() - 7
                    docs.documents.take(extraCount).forEach {
                        it.reference.delete()
                    }
                }
            }
    }

    fun getNotes(onResult: (List<Note>) -> Unit) {
        notesRef
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error fetching notes", error)
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull {
                    it.toObject(Note::class.java)
                } ?: emptyList()
                onResult(notes)
            }
    }
}
