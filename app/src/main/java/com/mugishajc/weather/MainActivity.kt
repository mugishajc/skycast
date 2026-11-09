package com.mugishajc.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.mugishajc.weather.ui.WeatherScreen
import com.mugishajc.weather.ui.WeatherViewModel
import com.mugishajc.weather.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels { WeatherViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WeatherTheme {
                WeatherScreen(viewModel)
            }
        }
    }
}
