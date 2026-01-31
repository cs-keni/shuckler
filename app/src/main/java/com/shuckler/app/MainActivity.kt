package com.shuckler.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.shuckler.app.navigation.ShucklerNavGraph
import com.shuckler.app.ui.theme.ShucklerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShucklerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShucklerNavGraph()
                }
            }
        }
    }
}
