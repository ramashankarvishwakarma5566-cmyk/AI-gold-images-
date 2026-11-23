package com.example.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

private val Context.themeDataStore by preferencesDataStore(name = "lumina_theme_prefs")

class ThemePreferencesRepository(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        val raw = prefs[themeModeKey]
        ThemeMode.entries.find { it.name == raw } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}

data class ThemeController(
    val themeMode: ThemeMode,
    val isDark: Boolean,
    val setThemeMode: (ThemeMode) -> Unit
)

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("LocalThemeController must be provided by LuminaThemeProvider")
}

@Composable
fun LuminaThemeProvider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        ThemePreferencesRepository(context.applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()
    val themeMode by repository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val systemIsDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val themeController = remember(themeMode, isDark) {
        ThemeController(
            themeMode = themeMode,
            isDark = isDark,
            setThemeMode = { targetMode ->
                coroutineScope.launch {
                    repository.setThemeMode(targetMode)
                }
            }
        )
    }

    CompositionLocalProvider(LocalThemeController provides themeController) {
        LuminaStudioTheme(darkTheme = isDark) {
            content()
        }
    }
}
