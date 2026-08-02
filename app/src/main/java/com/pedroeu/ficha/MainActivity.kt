package com.pedroeu.ficha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pedroeu.ficha.ui.FichaApp
import com.pedroeu.ficha.ui.i18n.LanguagePreference
import com.pedroeu.ficha.ui.layout.LayoutPreference
import com.pedroeu.ficha.ui.theme.FichaTheme
import com.pedroeu.ficha.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = (application as FichaApplication).repository
        val appearance = ThemePreference(this)
        val languages = LanguagePreference(this)
        val layouts = LayoutPreference(this)

        setContent {
            // Held here rather than inside the theme, so switching schemes redraws the whole
            // app at once and the choice outlives any one screen.
            var mode by remember { mutableStateOf(appearance.load()) }
            // The same reasoning for the language, and for the same reason it is loaded from
            // storage: both are settings that belong to the person, not to a character.
            var language by remember { mutableStateOf(languages.load()) }
            var layout by remember { mutableStateOf(layouts.load()) }

            FichaTheme(
                mode = mode,
                onModeChange = {
                    mode = it
                    appearance.save(it)
                },
            ) {
                FichaApp(
                    repository = repository,
                    language = language,
                    onLanguageChange = {
                        language = it
                        languages.save(it)
                    },
                    layout = layout,
                    onLayoutChange = {
                        layout = it
                        layouts.save(it)
                    },
                )
            }
        }
    }
}
