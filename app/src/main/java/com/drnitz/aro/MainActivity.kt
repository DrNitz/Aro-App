package com.drnitz.aro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.drnitz.aro.theme.AroTheme

class MainActivity : ComponentActivity() {

  private val planDeepLinkState = mutableStateOf<PlanDeepLink?>(null)
  private var handledPlanDeepLinkRequestId: Long? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    handledPlanDeepLinkRequestId = savedInstanceState
      ?.getLong(KEY_HANDLED_PLAN_DEEP_LINK, Long.MIN_VALUE)
      ?.takeUnless { it == Long.MIN_VALUE }
    acceptPlanDeepLink(intent)

    enableEdgeToEdge()
    setContent {
      AroTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            planDeepLink = planDeepLinkState.value,
            onPlanDeepLinkHandled = { requestId ->
              handledPlanDeepLinkRequestId = requestId
            },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    acceptPlanDeepLink(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    handledPlanDeepLinkRequestId?.let {
      outState.putLong(KEY_HANDLED_PLAN_DEEP_LINK, it)
    }
  }

  private fun acceptPlanDeepLink(intent: Intent?) {
    val link = PlanDeepLink.fromIntent(intent) ?: return
    // A configuration change can recreate the activity with the same notification
    // intent. Only replay it when it has not already opened the Plan view.
    if (link.requestId != handledPlanDeepLinkRequestId) {
      planDeepLinkState.value = link
    }
  }

  private companion object {
    const val KEY_HANDLED_PLAN_DEEP_LINK = "handled_plan_deep_link"
  }
}
