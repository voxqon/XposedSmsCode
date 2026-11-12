package io.github.magisk317.uikit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month

private const val DAYS_PER_WEEK = 7
private const val CELL_ASPECT_RATIO = 0.72f

/** A calendar month value that carries no platform-specific time APIs. */
data class YearMonth(val year: Int, val month: Int) {
    fun atDay(day: Int): LocalDate = LocalDate(year, month, day)
    fun lengthOfMonth(): Int {
        val leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        return when (month) {
            2 -> if (leap) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}

/** English short weekday labels indexed by ISO day number (1 = Monday .. 7 = Sunday). */
private val WEEKDAY_SHORT = arrayOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Stateless month grid. The parent owns dates, counts, selection, and actions. */
@Composable
fun CalendarMonthGrid(
    month: YearMonth,
    today: LocalDate,
    dayCounts: Map<LocalDate, Int>,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.ordinal + 1 - DayOfWeek.MONDAY.ordinal + 1 + DAYS_PER_WEEK) % DAYS_PER_WEEK
    val daysInMonth = month.lengthOfMonth()
    val rows = (leadingBlanks + daysInMonth + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (index in 0 until DAYS_PER_WEEK) {
                val isoDay = ((DayOfWeek.MONDAY.ordinal + 1 + index - 1) % DAYS_PER_WEEK) + 1
                Text(
                    text = WEEKDAY_SHORT[isoDay],
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        var dayNumber = 1
        repeat(rows) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(DAYS_PER_WEEK) { columnIndex ->
                    val cellIndex = rowIndex * DAYS_PER_WEEK + columnIndex
                    if (cellIndex < leadingBlanks || dayNumber > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(CELL_ASPECT_RATIO))
                    } else {
                        val date = month.atDay(dayNumber)
                        CalendarDayCell(
                            date = date,
                            count = dayCounts[date] ?: 0,
                            isToday = date == today,
                            isSelected = date == selectedDay,
                            isFuture = date > today,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectDay(date) },
                        )
                        dayNumber++
                    }
                }
            }
        }
    }
}

/** Stateless calendar day cell with an optional count and click action. */
@Composable
fun CalendarDayCell(
    date: LocalDate,
    count: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val hasRecords = count > 0
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasRecords -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }
    val dayColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(CELL_ASPECT_RATIO)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .then(if (hasRecords && !isFuture) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = dayColor,
            )
            if (hasRecords) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}
