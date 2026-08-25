package com.example.parkmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.parkmate.ui.navigation.ParkMateNavHost
import com.example.parkmate.ui.theme.ParkMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModelFactory = ParkMateViewModelFactory(
            (application as ParkMateApplication).vehicleRepository
        )

        setContent {
            ParkMateTheme {
                ParkMateNavHost(viewModelFactory = viewModelFactory)
            }
        }
    }
}
