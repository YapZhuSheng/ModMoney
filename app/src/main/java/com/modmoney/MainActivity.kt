package com.modmoney

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.modmoney.ui.GarageViewModel
import com.modmoney.ui.ModMoneyNavHost
import com.modmoney.ui.theme.ModMoneyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repo = (application as ModMoneyApp).repository

        setContent {
            // Dark by default; the toggle lets the user flip to light at runtime.
            var darkTheme by remember { mutableStateOf(true) }

            ModMoneyTheme(darkTheme = darkTheme) {
                val vm: GarageViewModel = viewModel(
                    factory = GarageViewModel.Factory(repo)
                )
                ModMoneyNavHost(
                    vm = vm,
                    darkTheme = darkTheme,
                    onToggleTheme = { darkTheme = !darkTheme }
                )
            }
        }
    }
}
