package com.drnitz.aro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.drnitz.aro.ui.main.MainScreen
import com.drnitz.aro.ui.planner.BodyPlannerScreen
import com.drnitz.aro.ui.vitals.VitalsScreen

@Composable
fun MainNavigation(
  planDeepLink: PlanDeepLink? = null,
  onPlanDeepLinkHandled: (Long) -> Unit = {},
) {
  val backStack = rememberNavBackStack(Main)

  LaunchedEffect(planDeepLink?.requestId) {
    if (planDeepLink == null) return@LaunchedEffect
    // A notification tap always returns to the main screen, even when the user left
    // the app on a deeper body-planner or vitals screen. MainScreen then opens Plan.
    while (backStack.isNotEmpty() && backStack.lastOrNull() != Main) {
      backStack.removeLastOrNull()
    }
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            planDeepLink = planDeepLink,
            onPlanDeepLinkHandled = onPlanDeepLinkHandled,
            modifier = Modifier
              .fillMaxSize()
              .safeDrawingPadding(),
          )
        }
        entry<BodyPlanner> {
          BodyPlannerScreen(
            initialTab = 0,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<ClinicalAudit> {
          BodyPlannerScreen(
            initialTab = 1,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
        entry<ClinicalRecords> { recordKey ->
          VitalsScreen(
            initialTab = recordKey.initialTab,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding(),
          )
        }
      },
  )
}

