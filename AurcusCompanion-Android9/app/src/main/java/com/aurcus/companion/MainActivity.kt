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
                    primary = Color(0xFF65D6C4),
                    secondary = Color(0xFFE2B76B),
                    background = Color(0xFF101318),
                    surface = Color(0xFF1B2028)
                )
            ) { CompanionScreen() }
        setupModListToggle()
    private fun setupModListToggle() {
        val toggleButton =
            findViewById<android.widget.Button>(R.id.btnToggleMods)

        val modList =
            findViewById<android.view.View>(R.id.modListContainer)

        toggleButton.setOnClickListener {
            val isExpanded =
                modList.visibility == android.view.View.VISIBLE

            if (isExpanded) {
                modList.visibility = android.view.View.GONE
                toggleButton.text = "Maximize Daftar Mod"
            } else {
                modList.visibility = android.view.View.VISIBLE
                toggleButton.text = "Minimize Daftar Mod"
        }
    }
}
