package com.loopworks.nibblemath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.loopworks.nibblemath.data.di.AppContainer
import com.loopworks.nibblemath.ui.navigation.NibbleMathNavHost
import com.loopworks.nibblemath.ui.theme.NibbleMathTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = remember { AppContainer(applicationContext) }
            LaunchedEffect(container) {
                container.catalogRepository.seedIfEmpty(applicationContext)
            }
            NibbleMathTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NibbleMathNavHost(container = container)
                }
            }
        }
    }
}
