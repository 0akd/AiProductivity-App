package com.arjundubey.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                text = "Just click on the top right three dashed button to open navigation drawer "
                        + "",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Justify,
                color = MaterialTheme.colorScheme.onBackground // 🔁 use themed text color
            )



            Spacer(modifier = Modifier.height(12.dp))

            // Paragraph 3
            Text(
                text = "drag your fingers towards right in fast pace will also open the navigation drawer",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Justify,
                color = MaterialTheme.colorScheme.primary // Optional: Give it an accent color
            )
        }
    }
}
