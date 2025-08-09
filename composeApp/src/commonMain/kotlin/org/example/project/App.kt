package org.example.project

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
//        ProvideNavigatorLifecycleKMPSupport {
            Navigator(StickManGameScreen())
//        }
    }
}