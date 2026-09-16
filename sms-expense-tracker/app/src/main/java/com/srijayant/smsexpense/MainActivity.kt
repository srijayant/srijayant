package com.srijayant.smsexpense

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.srijayant.smsexpense.ui.ExpenseApp
import com.srijayant.smsexpense.ui.ExpenseViewModel
import com.srijayant.smsexpense.ui.theme.SmsExpenseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmsExpenseTheme {
                val vm: ExpenseViewModel = viewModel()
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
                ExpenseApp(viewModel = vm, initiallyGranted = alreadyGranted)
            }
        }
    }
}
