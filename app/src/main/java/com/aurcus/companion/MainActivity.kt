package com.aurcus.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.aurcus.companion.ui.CompanionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF22B8F0),
                    onPrimary = Color(0xFF071321),
                    secondary = Color(0xFFE8B84B),
                    background = Color(0xFF071321),
                    surface = Color(0xFF10243A),
                    onSurface = Color(0xFFF5F8FC),
                    onBackground = Color(0xFFF5F8FC)
                )
            ) { CompanionScreen() }
        }
    }
}
