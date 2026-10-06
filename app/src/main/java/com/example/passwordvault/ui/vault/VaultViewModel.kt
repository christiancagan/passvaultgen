package com.example.passwordvault.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.domain.model.VaultEntry
import com.example.passwordvault.domain.model.VaultEntryDraft
import com.example.passwordvault.domain.model.VaultEntrySummary
import com.example.passwordvault.security.password.PasswordGenerator
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val passwordGenerator: PasswordGenerator,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    val searchQueryState: StateFlow<String> = searchQuery.asStateFlow()

    val entries: StateFlow<List<VaultEntrySummary>> =
        combine(repository.observeAll(), searchQuery) { all, query ->
            if (query.isBlank()) all
            else all.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<String>> =
        repository.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun generatePassword(): String = passwordGenerator.generate(com.example.passwordvault.security.password.PasswordOptions())

    fun add(draft: VaultEntryDraft, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { repository.add(draft) }
                onDone()
            } catch (e: Exception) {
                SecureLogger.e("add entry failed", e)
            }
        }
    }

    fun update(id: String, draft: VaultEntryDraft, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { repository.update(id, draft) }
                onDone()
            } catch (e: Exception) {
                SecureLogger.e("update entry failed", e)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) { repository.delete(id) }
            } catch (e: Exception) {
                SecureLogger.e("delete entry failed", e)
            }
        }
    }

    suspend fun getEntry(id: String): VaultEntry? =
        withContext(Dispatchers.Default) { repository.getEntry(id) }
}
