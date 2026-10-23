package com.drnitz.aro.ui.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drnitz.aro.data.db.DateProvider
import com.drnitz.aro.data.db.SystemDateProvider
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.domain.model.FastingSession
import com.drnitz.aro.domain.model.WaterLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Water and fasting history, kept separate from the live dashboard. */
sealed interface IntakeHistoryUiState {
    data object Loading : IntakeHistoryUiState

    data class Error(val throwable: Throwable) : IntakeHistoryUiState

    data class Success(
        val today: LocalDate,
        val waterLogs: List<WaterLog>,
        val fastingSessions: List<FastingSession>,
    ) : IntakeHistoryUiState {

        val totalWaterMl: Int get() = waterLogs.sumOf { it.amountMl }

        val todayWaterLogs: List<WaterLog>
            get() = waterLogs.filter { it.date == today }

        val last7DaysWaterLogs: List<WaterLog>
            get() = waterLogs.filter { !it.date.isBefore(today.minusDays(6)) }

        val last30DaysWaterLogs: List<WaterLog>
            get() = waterLogs.filter { !it.date.isBefore(today.minusDays(29)) }

        /** Newest day first; each day keeps its entries newest-first. */
        val waterLogsByDay: List<Pair<LocalDate, List<WaterLog>>>
            get() = waterLogs
                .groupBy { it.date }
                .entries
                .sortedByDescending { it.key }
                .map { it.key to it.value }

        val completedFasts: List<FastingSession>
            get() = fastingSessions.filterNot { it.isActive }

        val targetMetCount: Int
            get() = completedFasts.count { it.metTarget() }

        val averageCompletedMinutes: Long
            get() = if (completedFasts.isEmpty()) {
                0L
            } else {
                completedFasts.sumOf { it.durationMinutes() } / completedFasts.size
            }

        val totalCompletedMinutes: Long
            get() = completedFasts.sumOf { it.durationMinutes() }
    }
}

class IntakeHistoryViewModel(
    private val repository: AroRepository,
    private val dateProvider: DateProvider = SystemDateProvider(),
) : ViewModel() {

    private val history: Flow<IntakeHistoryUiState> = combine(
        repository.observeAllWaterLogs(),
        repository.observeFastHistory(FAST_HISTORY_LIMIT),
    ) { waterLogs, fastingSessions ->
        IntakeHistoryUiState.Success(
            today = dateProvider.today(),
            waterLogs = waterLogs,
            fastingSessions = fastingSessions,
        )
    }

    val uiState: StateFlow<IntakeHistoryUiState> = history
        .catch { emit(IntakeHistoryUiState.Error(it)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            IntakeHistoryUiState.Loading,
        )

    fun deleteWaterLog(id: String) {
        viewModelScope.launch { repository.deleteWaterLog(id) }
    }

    fun addWaterLog(date: LocalDate, amountMl: Int, loggedAt: java.time.Instant) {
        viewModelScope.launch { repository.addWaterLog(date, amountMl, loggedAt) }
    }

    fun updateWaterLog(id: String, date: LocalDate, amountMl: Int, loggedAt: java.time.Instant) {
        viewModelScope.launch { repository.updateWaterLog(id, date, amountMl, loggedAt) }
    }

    fun restoreWaterLog(log: WaterLog) {
        viewModelScope.launch { repository.restoreWaterLog(log) }
    }

    fun deleteFastingSession(id: String) {
        viewModelScope.launch { repository.deleteFastingSession(id) }
    }

    fun addFastingSession(
        targetHours: Int,
        startedAt: java.time.Instant,
        endedAt: java.time.Instant,
        note: String,
    ) {
        viewModelScope.launch {
            repository.addFastingSession(targetHours, startedAt, endedAt, note)
        }
    }

    fun updateFastingSession(
        id: String,
        targetHours: Int,
        startedAt: java.time.Instant,
        endedAt: java.time.Instant,
        note: String,
    ) {
        viewModelScope.launch {
            repository.updateFastingSession(id, targetHours, startedAt, endedAt, note)
        }
    }

    fun restoreFastingSession(session: FastingSession) {
        viewModelScope.launch { repository.restoreFastingSession(session) }
    }

    companion object {
        /**
         * High enough to include every realistic lifetime of fasts while still giving
         * the SQLite query a hard upper bound. At one fast a day this is 27 years.
         */
        private const val FAST_HISTORY_LIMIT = 10_000

        fun factory(
            repository: AroRepository,
            dateProvider: DateProvider = SystemDateProvider(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IntakeHistoryViewModel(repository, dateProvider) as T
            }
        }
    }
}
