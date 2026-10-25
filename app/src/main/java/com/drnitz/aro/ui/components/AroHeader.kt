package com.drnitz.aro.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.R
import com.drnitz.aro.ui.mascot.AroMascot
import com.drnitz.aro.ui.mascot.MascotMood

/**
 * Brand header: text-only wordmark with animated active section indicator and interactive mascot.
 *
 * "Aro" carries the brand and "by Dr.Nitz" the attribution. The active tab/section
 * name sits beside it with a smooth vertical slide transition, saving vertical space
 * across all main screens. The interactive companion mascot "Aro Bot" sits on the right.
 */
@Composable
fun AroHeader(
    onOpenNotifications: () -> Unit,
    onOpenGoals: () -> Unit,
    modifier: Modifier = Modifier,
    activeSection: String = "Diary",
    onOpenAssistant: (() -> Unit)? = null,
    mascotMood: MascotMood = MascotMood.IDLE,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.brand_attribution),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                        fontSize = 10.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            )

            Spacer(modifier = Modifier.width(10.dp))

            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(220)) { height -> height / 2 } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutVertically(animationSpec = tween(180)) { height -> -height / 2 } + fadeOut(animationSpec = tween(180)))
                        .using(SizeTransform(clip = false))
                },
                label = "HeaderSectionTransition",
                modifier = Modifier.weight(1f),
            ) { section ->
                Text(
                    text = section,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (onOpenAssistant != null) {
                AroMascot(
                    mood = mascotMood,
                    size = 42.dp,
                    onClick = onOpenAssistant,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }

            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.cd_notification_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onOpenGoals,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.cd_goals_and_profile),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
