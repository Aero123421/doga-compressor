package com.example.uiedvideocompacter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.uiedvideocompacter.ui.navigation.AppNavHost
import com.example.uiedvideocompacter.ui.theme.UIedVideoCompacterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UIedVideoCompacterTheme {
                AppNavHost()
            }
        }
    }
}