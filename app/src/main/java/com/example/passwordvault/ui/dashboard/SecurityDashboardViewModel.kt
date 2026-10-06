package com.example.passwordvault.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordvault.data.repository.VaultRepository
import com.example.passwordvault.security.password.PasswordStrengthAnalyzer
import com.example.passwordvault.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DashboardStats(
    val total: Int = 0,
    val weak: Int = 0,
    val reused: Int = 0,
    val old: Int = 0,
    val averageScore: Double = 0.0,
)

@HiltViewModel
class SecurityDashboardViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val analyzer: PasswordStrengthAnalyzer,
) : ViewModel() {

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val entries = withContext(Dispatchers.Default) { repository.getAllDecrypted() }
                _stats.value = computeStats(entries)
            } catch (e: Exception) {
                SecureLogger.e("dashboard refresh failed", e)
            }
        }
    }

    private fun computeStats(entries: List<com.example.passwordvault.domain.model.VaultEntry>): DashboardStats {
        if (entries.isEmpty()) return DashboardStats()

        val scores = entries.map { analyzer.analyze(it.password).score }
        val weak = scores.count { it <= 1 }

        val passwordCounts = entries.groupingBy { it.password }.eachCount()
        val reused = passwordCounts.values.count { it > 1 }

        val oldThreshold = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000)
        val old = entries.count { it.updatedAt < oldThreshold }

        val average = scores.average()

        return DashboardStats(
            total = entries.size,
            weak = weak,
            reused = reused,
            old = old,
            averageScore = average,
        )
    }
}
