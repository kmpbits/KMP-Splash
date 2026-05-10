package io.kmpbits.splash.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") }
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(it),
            contentAlignment = Alignment.Center,
        ) {
            Text("App is ready!")
        }
    }
}
