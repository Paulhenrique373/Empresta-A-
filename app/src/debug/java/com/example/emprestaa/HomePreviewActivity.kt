package com.example.emprestaa

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat

/** Preview-only launcher. It exists in the debug source set and is absent from release builds. */
class HomePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identity = runCatching {
            HomeIdentity.valueOf(intent.getStringExtra("identity") ?: "CIVIC")
        }.getOrDefault(HomeIdentity.CIVIC)
        val count = intent.getIntExtra("items", 0).coerceIn(0, 3)
        val barColor = if (identity == HomeIdentity.CIVIC) AndroidColor.rgb(246, 245, 239) else AndroidColor.rgb(244, 243, 238)
        window.statusBarColor = barColor
        window.navigationBarColor = AndroidColor.rgb(255, 255, 255)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent { HomePreviewScreen(identity, count) }
    }
}
