package com.arjundubey.app
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

// On your other screen
@Composable
fun OtherScreen(navController: NavController) {
    Column {
        Button(
            onClick = {
                // Navigate directly to timer with 300 seconds (5 minutes)
                navController.navigate("timer/300/Custom%20Timer/%234CAF50")
            }
        ) {
            Text("Start 5 Minute Timer")
        }

        Button(
            onClick = {
                // Navigate with different parameters
                navController.navigate("timer/600/Workout/%23FF5722")
            }
        ) {
            Text("Start 10 Minute Workout")
        }
    }
}