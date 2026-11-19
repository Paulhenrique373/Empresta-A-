package com.example.emprestaa

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat

/** Launcher de avaliação das direções de login, presente somente em debug. */
class LoginPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val direction = intent.getStringExtra("direction") ?: "A"
        val hasError = intent.getBooleanExtra("error", false)
        val darkPaper = direction.equals("B", ignoreCase = true)
        window.statusBarColor = if (darkPaper) AndroidColor.rgb(37, 32, 57) else AndroidColor.rgb(248, 243, 239)
        window.navigationBarColor = if (darkPaper) AndroidColor.rgb(37, 32, 57) else AndroidColor.rgb(248, 243, 239)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkPaper
            isAppearanceLightNavigationBars = !darkPaper
        }
        setContent { LoginDirectionPreview(direction, hasError) }
    }
}
