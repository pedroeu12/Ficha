package com.pedroeu.ficha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pedroeu.ficha.ui.FichaApp
import com.pedroeu.ficha.ui.theme.FichaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repository = (application as FichaApplication).repository
        setContent {
            FichaTheme {
                FichaApp(repository = repository)
            }
        }
    }
}
