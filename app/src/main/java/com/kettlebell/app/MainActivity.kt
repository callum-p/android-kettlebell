package com.kettlebell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kettlebell.app.ui.navigation.KettlebellRoot
import com.kettlebell.app.ui.theme.KettlebellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KettlebellTheme {
                KettlebellRoot()
            }
        }
    }
}
