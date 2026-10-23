package com.drnitz.aro

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object BodyPlanner : NavKey
@Serializable data object ClinicalAudit : NavKey
@Serializable data object DataManagement : NavKey
@Serializable data class ClinicalRecords(val initialTab: Int = 0) : NavKey
@Serializable data class IntakeHistory(val initialTab: Int = 0) : NavKey

