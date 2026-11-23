package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

object StreakkuRoutes {
    const val ROUTE_TODAY = "today"
    const val ROUTE_CALENDAR = "calendar"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_HABIT_FORM_BASE = "habit_form"
    const val ARG_HABIT_ID = "habitId"
    const val ROUTE_HABIT_FORM = "$ROUTE_HABIT_FORM_BASE?$ARG_HABIT_ID={$ARG_HABIT_ID}"

    fun habitFormRoute(habitId: String? = null): String {
        return if (habitId == null) {
            ROUTE_HABIT_FORM_BASE
        } else {
            "$ROUTE_HABIT_FORM_BASE?$ARG_HABIT_ID=$habitId"
        }
    }
}

enum class TopLevelTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    TODAY(
        route = StreakkuRoutes.ROUTE_TODAY,
        labelRes = R.string.nav_today,
        icon = Icons.Outlined.Home,
        testTag = "nav_tab_today"
    ),
    CALENDAR(
        route = StreakkuRoutes.ROUTE_CALENDAR,
        labelRes = R.string.nav_calendar,
        icon = Icons.Outlined.CalendarToday,
        testTag = "nav_tab_calendar"
    ),
    HISTORY(
        route = StreakkuRoutes.ROUTE_HISTORY,
        labelRes = R.string.nav_history,
        icon = Icons.Outlined.Schedule,
        testTag = "nav_tab_history"
    )
}
