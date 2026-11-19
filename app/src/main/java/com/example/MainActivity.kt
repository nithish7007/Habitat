package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PreferencesRepository
import com.example.data.ThemeMode
import com.example.ui.navigation.LifeOsApp
import com.example.ui.theme.LifeOsTheme
import com.example.worker.LifeOsNotificationCenter
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val pendingDeepLink = MutableStateFlow<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        extractDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            val prefsRepo = remember { PreferencesRepository(applicationContext) }
            val themeMode by prefsRepo.observeGlobalThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val deepLink by pendingDeepLink.collectAsStateWithLifecycle()
            LifeOsTheme(themeMode = themeMode) {
                LifeOsApp(
                    deepLink = deepLink,
                    onConsumeDeepLink = { pendingDeepLink.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLink(intent)
    }

    private fun extractDeepLink(intent: Intent?) {
        val route = intent?.getStringExtra(LifeOsNotificationCenter.EXTRA_DEEP_LINK_ROUTE)
        if (!route.isNullOrBlank()) {
            val arg = intent.getStringExtra(LifeOsNotificationCenter.EXTRA_DEEP_LINK_ARG).orEmpty()
            pendingDeepLink.value = route to arg
            intent.removeExtra(LifeOsNotificationCenter.EXTRA_DEEP_LINK_ROUTE)
            intent.removeExtra(LifeOsNotificationCenter.EXTRA_DEEP_LINK_ARG)
        }
    }
}
