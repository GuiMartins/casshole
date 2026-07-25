package com.faturaapp.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.local.InvoiceRepository
import com.faturaapp.data.local.entity.CategoryOverrideEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CategoryOverridesState {
    data object Loading : CategoryOverridesState()
    data class Loaded(val overrides: List<CategoryOverrideEntity>) : CategoryOverridesState()
    data class Error(val message: String) : CategoryOverridesState()
}

class CategoryOverridesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)

    private val _state = MutableStateFlow<CategoryOverridesState>(CategoryOverridesState.Loading)
    val state: StateFlow<CategoryOverridesState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = CategoryOverridesState.Loading
            try {
                _state.value = CategoryOverridesState.Loaded(repository.listCategoryOverrides())
            } catch (e: Exception) {
                _state.value = CategoryOverridesState.Error(e.message ?: "Erro ao carregar categorizações")
            }
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            repository.removeCategoryOverride(id)
            load()
        }
    }
}
