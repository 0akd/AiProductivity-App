package com.example.myapplication
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// --------------------- Data Models ---------------------



data class AwanRequest(
    val model: String = "Meta-Llama-3.1-70B-Instruct",
    val messages: List<AwanMessage>,
    val repetition_penalty: Double = 1.1,
    val temperature: Double = 0.7,
    val top_p: Double = 0.9,
    val top_k: Int = 40,
    val max_tokens: Int = 1024,
    val stream: Boolean = true // Enable streaming
)
data class StreamingChunk(
    val choices: List<StreamingChoice>?
)

data class StreamingChoice(
    val delta: StreamingDelta?
)

data class StreamingDelta(
    val content: String?
)

data class AwanResponse(val choices: List<AwanChoice>)

// --------------------- Retrofit API ---------------------



fun getAwanApi(): AwanApi {
    val apiKey = "0fb30cc8-f5d7-407b-ab38-279a8be29658"
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            chain.proceed(request)
        }
        .build()

    return Retrofit.Builder()
        .baseUrl("https://api.awanllm.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
        .create(AwanApi::class.java)
}


// --------------------- Chat Screen ---------------------

@Composable
fun ChatScreen(api: AwanApi = getAwanApi()) {
    val context = LocalContext.current
    var userInput by remember { mutableStateOf("") }
    var chatMessages by remember {
        mutableStateOf(ChatStorage.loadChat(context))
    }

    // Save whenever chatMessages updates
    LaunchedEffect(chatMessages) {
        ChatStorage.saveChat(context, chatMessages)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true
        ) {
            items(chatMessages.reversed()) {
                val label = if (it.role == "user") "🧑 You" else "🤖 AI"
                Text("$label: ${it.content}", modifier = Modifier.padding(8.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type your message...") }
            )

            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        val updatedMessages = chatMessages + AwanMessage("user", userInput)
                        chatMessages = updatedMessages
                        val currentInput = userInput
                        userInput = ""
                        sendMessage(api, updatedMessages) {
                            chatMessages = it
                        }
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }
    }
}


fun sendMessage(
    api: AwanApi,
    messages: List<AwanMessage>,
    onResult: (List<AwanMessage>) -> Unit
) {
    val request = AwanRequest(messages = messages)
    api.chat(request).enqueue(object : Callback<AwanResponse> {
        override fun onResponse(call: Call<AwanResponse>, response: Response<AwanResponse>) {
            val reply = response.body()?.choices?.firstOrNull()?.message
            onResult(messages + (reply ?: AwanMessage("assistant", "⚠️ No reply received.")))
        }

        override fun onFailure(call: Call<AwanResponse>, t: Throwable) {
            onResult(messages + AwanMessage("assistant", "❌ Error: ${t.message}"))
        }
    })
}
object ChatStorage {
    private const val PREF_NAME = "chat_prefs"
    private const val CHAT_KEY = "chat_messages"

    private val gson = Gson()

    fun saveChat(context: Context, messages: List<AwanMessage>) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(messages)
        prefs.edit().putString(CHAT_KEY, json).apply()
    }

    fun loadChat(context: Context): List<AwanMessage> {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(CHAT_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<AwanMessage>>() {}.type
        return gson.fromJson(json, type)
    }
}