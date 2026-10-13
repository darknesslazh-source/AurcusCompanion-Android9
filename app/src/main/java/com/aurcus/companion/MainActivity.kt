package com.aurcus.companion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.aurcus.companion.data.TabRequest
import com.aurcus.companion.data.Tools
import com.aurcus.companion.ui.CompanionScreen

class MainActivity : ComponentActivity() {
    // Overlay dapat meminta tab tertentu lewat extra Intent (lihat Tools.EXTRA_TAB).
    private var tabRequest by mutableStateOf<TabRequest?>(null)
    private var resumeCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabRequest = readTabRequest(intent)
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
            ) { CompanionScreen(tabRequest = tabRequest, resumeCount = resumeCount) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        tabRequest = readTabRequest(intent)
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
    }

    private fun readTabRequest(source: Intent?): TabRequest? {
        if (source == null || !source.hasExtra(Tools.EXTRA_TAB)) return null
        val request = TabRequest(source.getIntExtra(Tools.EXTRA_TAB, 0), System.nanoTime())
        source.removeExtra(Tools.EXTRA_TAB) // jangan ulangi saat Activity dibuat ulang (rotasi)
        return request
    }
}
