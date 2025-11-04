package com.arjundubey.app

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DrawerButton(text: String, onClick: () -> Unit) {
    Text(
        text = "${getEmojiForScreen(text)} $text",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.medium)
            .padding(12.dp), // inner padding inside border
        style = MaterialTheme.typography.bodyLarge
    )
}