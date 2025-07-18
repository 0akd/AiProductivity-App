package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // 🔁 use themed background
            .padding(top = 80.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // Blog Heading
            Text(
                text = "Welcome Guys - below I have given instructions please read it CAREFULLY ",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                color = MaterialTheme.colorScheme.onBackground // 🔁 use themed text color
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Paragraph 1
            Text(
                text = "Swiping right ->->--->-->> opens drawer from where you can navigate. "
                        + "Or you can just click on the top right button.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Justify,
                color = MaterialTheme.colorScheme.onBackground // 🔁 use themed text color
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Paragraph 2
            Text(
                text = "In todo app, if you LONG PRESS on your todo it adds it to the Focus tab "
                        + "which can be accessed from the bottom bar. Focus tab reminds you to stick to that particular task.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Justify,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Paragraph 3
            Text(
                text = "A Digital Product - delivered by Dubey Industries",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Justify,
                color = MaterialTheme.colorScheme.primary // Optional: Give it an accent color
            )
        }
    }
}
