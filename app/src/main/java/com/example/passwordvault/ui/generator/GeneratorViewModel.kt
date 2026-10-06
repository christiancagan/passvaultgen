package com.example.passwordvault.ui.generator

import androidx.lifecycle.ViewModel
import com.example.passwordvault.security.password.PasswordGenerator
import com.example.passwordvault.security.password.PasswordOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class GeneratorUiState(
    val password: String = "",
    val entropyBits: Double = 0.0,
    val options: PasswordOptions = PasswordOptions(),
    val passphraseMode: Boolean = false,
    val wordCount: Int = 6,
)

@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val generator: PasswordGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    fun generate() {
        val state = _uiState.value
        if (state.passphraseMode) {
            val passphrase = generator.generatePassphrase(wordCount = state.wordCount)
            val entropy = state.wordCount * log2(PasswordGenerator.WORD_LIST.size.toDouble())
            _uiState.value = state.copy(password = passphrase, entropyBits = entropy)
        } else {
            val password = generator.generate(state.options)
            val charsetSize = generator.charsetSize(state.options)
            val entropy = generator.estimateEntropy(state.options.length, charsetSize)
            _uiState.value = state.copy(password = password, entropyBits = entropy)
        }
    }

    fun updateOptions(options: PasswordOptions) {
        _uiState.value = _uiState.value.copy(options = options)
    }

    fun setPassphraseMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(passphraseMode = enabled)
    }

    fun setWordCount(count: Int) {
        _uiState.value = _uiState.value.copy(wordCount = count)
    }

    private fun log2(x: Double): Double = kotlin.math.log2(x)
}
