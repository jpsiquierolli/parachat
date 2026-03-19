package com.example.parachat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.example.parachat.navigation.ParachatNavHost
import com.example.parachat.ui.theme.ParachatTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()
        setContent {
            Box (
                modifier = Modifier
                    .safeDrawingPadding()
            ) {
                ParachatTheme {
                    ParachatNavHost()
                }
            }
        }
    }
}