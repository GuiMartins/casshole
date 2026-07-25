package com.moneyhole.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.moneyhole.R

enum class MainScreen {
    HOME,
    INVOICES_BY_CARD,
    COMPARE,
    SETTINGS,
}

@Composable
fun FloatingNavigationBar(
    currentScreen: MainScreen,
    onGoHome: () -> Unit,
    onViewByCard: () -> Unit,
    onCompare: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(6.dp),
        ) {
            NavigationItem(
                icon = Icons.Filled.Home,
                description = stringResource(R.string.nav_home_cd),
                selected = currentScreen == MainScreen.HOME,
                onClick = onGoHome,
            )
            NavigationItem(
                icon = Icons.Filled.CreditCard,
                description = stringResource(R.string.nav_invoices_by_card),
                selected = currentScreen == MainScreen.INVOICES_BY_CARD,
                onClick = onViewByCard,
            )
            NavigationItem(
                icon = Icons.Filled.BarChart,
                description = stringResource(R.string.action_compare),
                selected = currentScreen == MainScreen.COMPARE,
                onClick = onCompare,
            )
            NavigationItem(
                icon = Icons.Filled.Settings,
                description = stringResource(R.string.nav_settings),
                selected = currentScreen == MainScreen.SETTINGS,
                onClick = onSettings,
            )
        }
    }
}

@Composable
private fun NavigationItem(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = iconColor)
    }
}
