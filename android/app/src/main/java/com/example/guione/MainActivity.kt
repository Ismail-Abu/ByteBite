package com.example.guione

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.guione.meal.ui.ByteBiteApp

/**
 * The single entry point. The whole app is the persisted meal experience
 * ([ByteBiteApp]); there is no sample-data chooser in the production flow.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ByteBiteApp() }
    }
}
