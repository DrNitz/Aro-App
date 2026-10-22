package com.drnitz.aro.ui.vitals

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drnitz.aro.data.db.DateProvider
import com.drnitz.aro.data.db.SystemDateProvider
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.domain.clinical.ClinicalAuditEngine
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.export.ClinicalPdfGenerator
import com.drnitz.aro.export.ClinicalReportData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface VitalsUiState {
    data object Loading : VitalsUiState

    data class Success(
        val profile: UserProfile,
        val vitals: List<VitalsEntry>,
        val consultations: List<DoctorConsultation>,
        val latestVitals: VitalsEntry?,
        val recentTotals: NutrientTotals,
        val molarNaKRatio: Double?,
        val sodiumMg: Double?,
        val potassiumMg: Double?,
        val clinicalFindings: List<String>,
    ) : VitalsUiState
}

class VitalsViewModel(
    private val repository: AroRepository,
    private val dateProvider: DateProvider = SystemDateProvider(),
) : ViewModel() {

    private val selectedTabFlow = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = selectedTabFlow

    val uiState: StateFlow<VitalsUiState> = combine(
        repository.observeProfile(),
        repository.observeAllVitals(),
        repository.observeAllConsultations(),
        repository.observeDayTotals(dateProvider.today()),
    ) { profileRaw, vitalsList, consultsList, todayTotals ->
        val profile = profileRaw ?: UserProfile.DEFAULT
        val latest = vitalsList.maxByOrNull { it.timestamp }

        val sodium = todayTotals.sodiumMg
        val potassium = todayTotals.potassiumMg
        val ratio = ClinicalAuditEngine.naKRatio(sodium, potassium).molarRatio

        val auditResult = ClinicalAuditEngine.assess(todayTotals)
        val findingsText = auditResult.findings.map { it.message }

        VitalsUiState.Success(
            profile = profile,
            vitals = vitalsList,
            consultations = consultsList,
            latestVitals = latest,
            recentTotals = todayTotals,
            molarNaKRatio = ratio,
            sodiumMg = sodium,
            potassiumMg = potassium,
            clinicalFindings = findingsText,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VitalsUiState.Loading,
    )

    fun selectTab(index: Int) {
        selectedTabFlow.value = index
    }

    fun logVitals(entry: VitalsEntry) {
        viewModelScope.launch {
            repository.logVitals(entry)
        }
    }

    fun deleteVitals(id: String) {
        viewModelScope.launch {
            repository.deleteVitals(id)
        }
    }

    fun logConsultation(consultation: DoctorConsultation) {
        viewModelScope.launch {
            repository.logConsultation(consultation)
        }
    }

    fun deleteConsultation(id: String) {
        viewModelScope.launch {
            repository.deleteConsultation(id)
        }
    }

    fun exportPdf(context: Context, rangeDays: Int, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            val today = dateProvider.today()
            val fromDate = if (rangeDays > 0) today.minusDays(rangeDays.toLong()) else LocalDate.of(2000, 1, 1)

            val vitalsInRange = repository.getVitalsInRange(fromDate, today)
            val consultsInRange = repository.getConsultationsInRange(fromDate, today)

            val dayTotalsInRange = repository.observeTotalsInRange(fromDate, today).stateIn(viewModelScope).value
            val avgCal = if (dayTotalsInRange.isNotEmpty()) {
                dayTotalsInRange.map { it.totals.calories.toInt() }.average().toInt()
            } else 2000

            val avgProt = if (dayTotalsInRange.isNotEmpty()) {
                dayTotalsInRange.map { it.totals.proteinG }.average()
            } else 120.0

            val avgCarb = if (dayTotalsInRange.isNotEmpty()) {
                dayTotalsInRange.map { it.totals.carbsG }.average()
            } else 200.0

            val avgFat = if (dayTotalsInRange.isNotEmpty()) {
                dayTotalsInRange.map { it.totals.fatG }.average()
            } else 65.0

            val avgFiber = if (dayTotalsInRange.isNotEmpty()) {
                dayTotalsInRange.map { it.totals.fiberG }.average()
            } else 30.0

            val avgSod = if (dayTotalsInRange.isNotEmpty()) {
                dayTotalsInRange.map { it.totals.sodiumMg }.average()
            } else 2100.0

            val avgPot = if (dayTotalsInRange.isNotEmpty()) {
                dayTotalsInRange.map { it.totals.potassiumMg }.average()
            } else 2800.0

            val naK = ClinicalAuditEngine.naKRatio(avgSod, avgPot).molarRatio

            val (s, currentState) = when (val s = uiState.value) {
                is VitalsUiState.Success -> Pair(s, s)
                else -> Pair(null, null)
            }

            val rangeText = if (rangeDays > 0) {
                "Last $rangeDays Days (${fromDate.format(DateTimeFormatter.ofPattern("MMM d"))} – ${today.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))})"
            } else "All Recorded History"

            val reportData = ClinicalReportData(
                profile = currentState?.profile ?: UserProfile.DEFAULT,
                dateRangeText = rangeText,
                generatedDate = today.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                vitals = if (vitalsInRange.isNotEmpty()) vitalsInRange else (currentState?.vitals ?: emptyList()),
                consultations = if (consultsInRange.isNotEmpty()) consultsInRange else (currentState?.consultations ?: emptyList()),
                averageCalories = avgCal,
                averageProtein = avgProt,
                averageCarbs = avgCarb,
                averageFat = avgFat,
                averageFiber = avgFiber,
                molarNaKRatio = naK,
                sodiumMg = avgSod,
                potassiumMg = avgPot,
                clinicalFindings = currentState?.clinicalFindings ?: emptyList(),
            )

            val file = ClinicalPdfGenerator.generate(context, reportData)
            onComplete(file)
        }
    }

    companion object {
        fun factory(
            repository: AroRepository,
            dateProvider: DateProvider = SystemDateProvider(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VitalsViewModel(repository, dateProvider) as T
            }
        }
    }
}
