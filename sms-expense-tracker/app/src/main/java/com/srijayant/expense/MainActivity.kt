package com.srijayant.expense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier.Modifier
import com.srijayant.expense.ui.screens.HomeScreen
import com.srijayant.expense.ui.theme.ExpenseTheme
import com.srijayant.expense.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpenseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(viewModel)
                }
            }
        }
    }
}
