package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.CPDMSRepository

class CPDMSViewModelFactory(private val repository: CPDMSRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CPDMSViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CPDMSViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
