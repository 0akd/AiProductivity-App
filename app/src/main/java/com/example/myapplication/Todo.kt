package com.example.myapplication

// MainActivity.kt
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

data class Question(
    val id: Int,
    val text: String,
    var isChecked: Boolean = false
)

data class Card(
    val id: String,
    val title: String,
    val questions: List<Question>,
    var isCompleted: Boolean = false,
    var counter: Int = 0,
    var timerSeconds: Long = 0L
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardManagerApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("card_prefs", Context.MODE_PRIVATE) }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, sharedPrefs)
        }
        composable("card_detail/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
            CardDetailScreen(navController, sharedPrefs, cardId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, sharedPrefs: SharedPreferences) {
    var cards by remember { mutableStateOf(loadCards(sharedPrefs)) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var showEditCardDialog by remember { mutableStateOf(false) }
    var cardToEdit by remember { mutableStateOf<Card?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Manager") }
            )
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = { showAddCardDialog = true },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Card")
                }

                FloatingActionButton(
                    onClick = {
                        cards = cards.map { card ->
                            card.copy(
                                questions = card.questions.map { it.copy(isChecked = false) },
                                isCompleted = false
                            )
                        }
                        saveCards(sharedPrefs, cards)
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset All Checkboxes")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cards) { card ->
                CardItem(
                    card = card,
                    onClick = {
                        navController.navigate("card_detail/${card.id}")
                    },
                    onEdit = {
                        cardToEdit = card
                        showEditCardDialog = true
                    },
                    onDelete = {
                        cards = cards.filter { it.id != card.id }
                        saveCards(sharedPrefs, cards)
                    }
                )
            }
        }
    }

    if (showAddCardDialog) {
        AddCardDialog(
            onDismiss = { showAddCardDialog = false },
            onConfirm = { cardTitle, questionTexts ->
                val newCard = createNewCard(cardTitle, questionTexts)
                cards = cards + newCard
                saveCards(sharedPrefs, cards)
                showAddCardDialog = false
            }
        )
    }

    if (showEditCardDialog && cardToEdit != null) {
        EditCardDialog(
            card = cardToEdit!!,
            onDismiss = {
                showEditCardDialog = false
                cardToEdit = null
            },
            onConfirm = { updatedCard ->
                cards = cards.map { if (it.id == updatedCard.id) updatedCard else it }
                saveCards(sharedPrefs, cards)
                showEditCardDialog = false
                cardToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardItem(
    card: Card,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (card.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Card",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Card",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (!card.isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${card.questions.count { it.isChecked }}/${card.questions.size} completed",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Counter: ${card.counter}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Time: ${formatTime(card.timerSeconds)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(navController: NavController, sharedPrefs: SharedPreferences, cardId: String) {
    var cards by remember { mutableStateOf(loadCards(sharedPrefs)) }
    val card = cards.find { it.id == cardId } ?: return
    val context = LocalContext.current
    var questions by remember { mutableStateOf(card.questions) }
    var counter by remember { mutableStateOf(card.counter) }
    var timerSeconds by remember { mutableStateOf(card.timerSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Timer effect
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000L)
            timerSeconds++
            updateCardTimerAndCounter(sharedPrefs, cardId, counter, timerSeconds)
        }
    }

    LaunchedEffect(questions) {
        if (questions.all { it.isChecked }) {
            val updatedCards = cards.map {
                if (it.id == cardId) it.copy(
                    isCompleted = true,
                    questions = questions,
                    counter = counter,
                    timerSeconds = timerSeconds
                )
                else it
            }
            cards = updatedCards
            saveCards(sharedPrefs, updatedCards)
            isTimerRunning = false
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.title) },
                navigationIcon = {
                    IconButton(onClick = {
                        updateCardTimerAndCounter(sharedPrefs, cardId, counter, timerSeconds)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = { isTimerRunning = !isTimerRunning },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isTimerRunning) "Pause Timer" else "Start Timer"
                    )
                }

                FloatingActionButton(
                    onClick = {
                        questions = questions.map { it.copy(isChecked = false) }
                        counter = 0
                        timerSeconds = 0L
                        isTimerRunning = false
                        val updatedCards = cards.map {
                            if (it.id == cardId) it.copy(
                                isCompleted = false,
                                questions = questions,
                                counter = counter,
                                timerSeconds = timerSeconds
                            )
                            else it
                        }
                        cards = updatedCards
                        saveCards(sharedPrefs, updatedCards)
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Counter and Timer Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Counter",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = counter.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = {
                                counter--
                                updateCardTimerAndCounter(sharedPrefs, cardId, counter, timerSeconds)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("-")
                        }
                        Button(
                            onClick = {
                                counter++
                                updateCardTimerAndCounter(sharedPrefs, cardId, counter, timerSeconds)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("+")
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Timer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatTime(timerSeconds),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isTimerRunning) "Running" else "Stopped",
                            fontSize = 12.sp,
                            color = if (isTimerRunning) Color.Green else Color.Gray
                        )
                        TimerAndCounterEditor(
                            sharedPrefs = sharedPrefs,
                            cardId = cardId,
                            onUpdated = { updatedCounter, updatedTimer ->
                                counter = updatedCounter
                                timerSeconds = updatedTimer
                                Toast.makeText(context, "Card updated!", Toast.LENGTH_SHORT).show()
                            }
                        )



                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(questions.size) { index ->
                    val question = questions[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = question.isChecked,
                            onCheckedChange = { isChecked ->
                                questions = questions.toMutableList().apply {
                                    this[index] = question.copy(isChecked = isChecked)
                                }
                                val updatedCards = cards.map {
                                    if (it.id == cardId) it.copy(
                                        questions = questions,
                                        counter = counter,
                                        timerSeconds = timerSeconds
                                    )
                                    else it
                                }
                                cards = updatedCards
                                saveCards(sharedPrefs, updatedCards)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = question.text,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun TimerAndCounterEditor(
    sharedPrefs: SharedPreferences,
    cardId: String,
    onUpdated: (Int, Long) -> Unit
) {
    val context = LocalContext.current

    var timerText by remember { mutableStateOf("") }
    var counterText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = timerText,
            onValueChange = { timerText = it },
            label = { Text("Timer Seconds") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = counterText,
            onValueChange = { counterText = it },
            label = { Text("Counter") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val updatedTimer = timerText.toLongOrNull() ?: 0L
            val updatedCounter = counterText.toIntOrNull() ?: 0

            updateCardTimerAndCounter(sharedPrefs, cardId, updatedCounter, updatedTimer)
            onUpdated(updatedCounter, updatedTimer)
        }) {
            Text("Update Card")
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var cardTitle by remember { mutableStateOf("") }
    var questionTexts by remember { mutableStateOf(listOf("were you at the verge of death while trying hard to do it , if not go back again", "Question 2", "Question 3")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = cardTitle,
                    onValueChange = { cardTitle = it },
                    label = { Text("Card Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Questions:", fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = {
                            questionTexts = questionTexts + "New Question"
                        }
                    ) {
                        Text("+ Add Question")
                    }
                }

                questionTexts.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                questionTexts = questionTexts.toMutableList().apply {
                                    this[index] = newText
                                }
                            },
                            label = {
                                Text(if (index == 0) "First Question" else "Question ${index + 1}")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                        )
                        if (questionTexts.size > 3) {
                            IconButton(
                                onClick = {
                                    questionTexts = questionTexts.filterIndexed { i, _ -> i != index }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Question")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (cardTitle.isNotBlank()) {
                        onConfirm(cardTitle, questionTexts.filter { it.isNotBlank() })
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(
    card: Card,
    onDismiss: () -> Unit,
    onConfirm: (Card) -> Unit
) {
    var cardTitle by remember { mutableStateOf(card.title) }
    var questionTexts by remember {
        mutableStateOf(card.questions.map { it.text }.toMutableList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Card") },
        text = {
            Column {
                OutlinedTextField(
                    value = cardTitle,
                    onValueChange = { cardTitle = it },
                    label = { Text("Card Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Questions:", fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = {
                            questionTexts = questionTexts.toMutableList().apply {
                                add("New Question")
                            }
                        }
                    ) {
                        Text("+ Add Question")
                    }
                }

                questionTexts.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { newText ->
                                questionTexts = questionTexts.toMutableList().apply {
                                    this[index] = newText
                                }
                            },
                            label = { Text("Question ${index + 1}") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp)
                        )
                        if (questionTexts.size > 3) {
                            IconButton(
                                onClick = {
                                    questionTexts = questionTexts.filterIndexed { i, _ -> i != index }.toMutableList()

                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Question")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (cardTitle.isNotBlank()) {
                        val updatedQuestions = questionTexts
                            .filter { it.isNotBlank() }
                            .mapIndexed { index, text ->
                                Question(
                                    id = index + 1,
                                    text = text,
                                    isChecked = if (index < card.questions.size) card.questions[index].isChecked else false
                                )
                            }
                        onConfirm(card.copy(title = cardTitle, questions = updatedQuestions))
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun createNewCard(title: String = "", questionTexts: List<String> = emptyList()): Card {
    val cardNumber = System.currentTimeMillis().toString().takeLast(3)
    val cardTitle = if (title.isBlank()) "Card $cardNumber" else title
    val questions = if (questionTexts.isEmpty()) {
        listOf(
            Question(1, "were you at the verge of death while trying hard to do it , if not go back again"),
            Question(2, "Question 2"),
            Question(3, "Question 3")
        )
    } else {
        questionTexts.mapIndexed { index, text ->
            Question(index + 1, text)
        }
    }

    return Card(
        id = "card_$cardNumber",
        title = cardTitle,
        questions = questions,
        counter = 0,
        timerSeconds = 0L
    )
}

fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

fun updateCardTimerAndCounter(sharedPrefs: SharedPreferences, cardId: String, counter: Int, timerSeconds: Long) {
    val cards = loadCards(sharedPrefs)
    val updatedCards = cards.map { card ->
        if (card.id == cardId) {
            card.copy(counter = counter, timerSeconds = timerSeconds)
        } else {
            card
        }
    }
    saveCards(sharedPrefs, updatedCards)
}

fun loadCards(sharedPrefs: SharedPreferences): List<Card> {
    val cardsJson = sharedPrefs.getString("cards", "[]")
    val gson = Gson()
    val type = object : TypeToken<List<Card>>() {}.type
    return gson.fromJson(cardsJson, type) ?: emptyList()
}

fun saveCards(sharedPrefs: SharedPreferences, cards: List<Card>) {
    val gson = Gson()
    val cardsJson = gson.toJson(cards)
    sharedPrefs.edit().putString("cards", cardsJson).apply()
}

