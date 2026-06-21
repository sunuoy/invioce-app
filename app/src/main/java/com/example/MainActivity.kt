package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.InvoiceViewModel
import com.example.ui.MainAppNavigation
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = androidx.compose.ui.platform.LocalContext.current
      val prefs = remember { context.getSharedPreferences("app_settings", MODE_PRIVATE) }
      var themeMode by remember { mutableStateOf(prefs.getString("app_theme_mode", "system") ?: "system") }

      DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
          if (key == "app_theme_mode") {
            themeMode = sharedPreferences.getString(key, "system") ?: "system"
          }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
          prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
      }

      val isDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          val invoiceViewModel: InvoiceViewModel = viewModel()
          MainAppNavigation(viewModel = invoiceViewModel)
        }
      }
    }
  }
}
