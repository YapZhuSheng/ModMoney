package com.modmoney.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.modmoney.data.ModPriority
import com.modmoney.data.ModStatus
import com.modmoney.data.WarningSeverity
import com.modmoney.rules.CompatWarning
import com.modmoney.ui.theme.AccentColors

/** Small pill-shaped label with a tinted background. */
@Composable
fun TagChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .border(
                width = if (filled) 0.dp else 1.dp,
                color = if (filled) Color.Transparent else color.copy(alpha = 0.45f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (filled) Color(0xFF14181F) else color
        )
    }
}

@Composable
fun StatusChip(status: ModStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        ModStatus.PLANNED -> AccentColors.planned
        ModStatus.BOUGHT -> AccentColors.bought
        ModStatus.INSTALLED -> AccentColors.installed
    }
    TagChip(text = status.label, color = color, modifier = modifier)
}

@Composable
fun PriorityChip(priority: ModPriority, modifier: Modifier = Modifier) {
    val color = when (priority) {
        ModPriority.HIGH -> AccentColors.high
        ModPriority.MEDIUM -> AccentColors.medium
        ModPriority.LOW -> AccentColors.low
    }
    TagChip(text = "${priority.label} priority", color = color, modifier = modifier)
}

fun severityColor(severity: WarningSeverity): Color = when (severity) {
    WarningSeverity.INFO -> AccentColors.info
    WarningSeverity.CAUTION -> AccentColors.caution
    WarningSeverity.CRITICAL -> AccentColors.critical
}

/** Expanded card describing one compatibility warning. */
@Composable
fun WarningCard(warning: CompatWarning, modifier: Modifier = Modifier) {
    val color = severityColor(warning.severity)
    val icon = when (warning.severity) {
        WarningSeverity.INFO -> Icons.Default.Info
        WarningSeverity.CAUTION -> Icons.Default.WarningAmber
        WarningSeverity.CRITICAL -> Icons.Default.ErrorOutline
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = warning.severity.label,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = warning.title,
                style = MaterialTheme.typography.labelLarge,
                color = color
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = warning.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Rounded, animated progress track used by the dashboard and car cards. */
@Composable
fun BudgetBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    trackHeight: Int = 10,
    barColor: Color = MaterialTheme.colorScheme.primary,
    /** Optional secondary marker drawn behind the main bar (projected spend). */
    ghostFraction: Float? = null
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(650),
        label = "budget"
    )
    val animatedGhost by animateFloatAsState(
        targetValue = (ghostFraction ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(650),
        label = "budgetGhost"
    )
    val color by animateColorAsState(barColor, label = "budgetColor")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        if (ghostFraction != null && animatedGhost > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animatedGhost)
                    .fillMaxSize()
                    .background(color.copy(alpha = 0.28f))
            )
        }
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxSize()
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

/** Centered empty-state block. */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Label/value pair used across detail surfaces. */
@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor
        )
    }
}
