package com.casshole.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.casshole.R
import com.casshole.data.local.InvoiceRepository
import com.casshole.data.local.entity.CategoryOverrideEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [transactionCount] is how many stored transactions the override covers - i.e. the reach of a retroactive edit. */
data class CategoryOverrideItem(
    val override: CategoryOverrideEntity,
    val transactionCount: Int,
)

sealed class CategoryOverridesState {
    data object Loading : CategoryOverridesState()
    data class Loaded(val overrides: List<CategoryOverrideItem>) : CategoryOverridesState()
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
                val counts = repository.countTransactionsByDescription()
                _state.value = CategoryOverridesState.Loaded(
                    repository.listCategoryOverrides().map { override ->
                        CategoryOverrideItem(override, counts[override.description] ?: 0)
                    }
                )
            } catch (e: Exception) {
                _state.value = CategoryOverridesState.Error(e.message ?: getApplication<Application>().getString(R.string.error_load_category_overrides))
            }
        }
    }

    /** Retroactive by design: changing the rule here recategorizes every transaction already stored under it. */
    fun updateCategory(description: String, category: String) {
        viewModelScope.launch {
            try {
                repository.applyCategoryToDescription(description, category)
                load()
            } catch (e: Exception) {
                _state.value = CategoryOverridesState.Error(e.message ?: getApplication<Application>().getString(R.string.error_update_category))
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
