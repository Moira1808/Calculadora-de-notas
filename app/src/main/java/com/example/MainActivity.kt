package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.SubjectRepository
import com.example.ui.SubjectApp
import com.example.ui.SubjectViewModel
import com.example.ui.SubjectViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize persistent offline-first Room Database
    val database = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "salvacion_curriculum_database"
    )
    .fallbackToDestructiveMigration()
    .build()

    val repository = SubjectRepository(database.subjectDao())
    val viewModelFactory = SubjectViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[SubjectViewModel::class.java]

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          SubjectApp(viewModel = viewModel)
        }
      }
    }
  }
}
