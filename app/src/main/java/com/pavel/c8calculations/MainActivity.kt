package com.pavel.c8calculations

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.pavel.c8calculations.ui.navigation.AppNavigation
import com.pavel.c8calculations.ui.theme.C8CalculationsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { C8CalculationsTheme { AppNavigation() } }
    }
}
