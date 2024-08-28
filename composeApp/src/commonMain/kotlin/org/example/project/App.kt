package org.example.project

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import com.sample.kstatemachine_compose_sample.StickManGameScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
//import cafe.adriel.voyager.jetpack.ProvideNavigatorLifecycleKMPSupport

//@OptIn(ExperimentalVoyagerApi::class)
@Composable
@Preview
fun App() {
    MaterialTheme {
//        ProvideNavigatorLifecycleKMPSupport {
            Navigator(StickManGameScreen())
//        }
    }
}