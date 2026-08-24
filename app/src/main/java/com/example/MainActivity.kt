package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.database.CPDMSDatabase
import com.example.data.repository.CPDMSRepository
import com.example.ui.screens.CPDMSAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CPDMSViewModel
import com.example.ui.viewmodel.CPDMSViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database
        val db = Room.databaseBuilder(
            applicationContext,
            CPDMSDatabase::class.java,
            "cpdms_database"
        ).fallbackToDestructiveMigration().build()

        // Create Repository & ViewModel
        val repository = CPDMSRepository(db)
        val viewModelFactory = CPDMSViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[CPDMSViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CPDMSAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}
