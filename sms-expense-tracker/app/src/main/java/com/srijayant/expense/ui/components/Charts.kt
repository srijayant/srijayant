package com.srijayant.expense.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srijayant.expense.data.CategoryTotal
import com.srijayant.expense.data.ExpenseCategory
import kotlin.math.min

private val categoryColors = mapOf(
    ExpenseCategory.FOOD to Color(0xFF2D8A6A),
    ExpenseCategory.SHOPPING to Color(0xFFC45C26),
    ExpenseCategory.TRAVEL to Color(0xFF1D6F9F),
    ExpenseCategory.BILLS to Color(0xFF6B5B95),
    ExpenseCategory.ENTERTAINMENT to Color(0xFFD4A017),
    ExpenseCategory.HEALTH to Color(0xFFB42318),
    ExpenseCategory.TRANSFER to Color(0xFF4A6670),
    ExpenseCategory.ATM to Color(0xFF5C6B5A),
    ExpenseCategory.OTHER to Color(0xFF8AA297)
)

fun colorFor(category: ExpenseCategory): Color =
    categoryColors[category] ?: Color(0xFF8AA297)

@Composable
fun CategoryDonut(
    totals: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    val total = totals.sumOf { it.total }.toFloat().coerceAtLeast(0.01f)
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(progress, tween(900), label = "donut")

    LaunchedEffect(totals) { progress = 1f }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val stroke = 22.dp.toPx()
                val diameter = min(size.width, size.height) - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = Color(0xFFE4EEE9),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                var start = -90f
                totals.forEach { item ->
                    val sweep = (item.total.toFloat() / total) * 360f * animated
                    drawArc(
                        color = colorFor(item.category),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${totals.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            totals.take(5).forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(colorFor(item.category), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.category.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = formatInr(item.total),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun DailySpendBars(
    dailyTotals: Map<Int, Double>,
    daysInMonth: Int,
    modifier: Modifier = Modifier
) {
    val max = (dailyTotals.values.maxOrNull() ?: 1.0).toFloat().coerceAtLeast(1f)
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(progress, tween(700), label = "bars")
    LaunchedEffect(dailyTotals) { progress = 1f }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Daily spend",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (day in 1..daysInMonth) {
                val value = (dailyTotals[day] ?: 0.0).toFloat()
                val fraction = (value / max) * animated
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height((8 + 80 * fraction).dp)
                        .background(
                            if (value > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                )
            }
        }
    }
}

fun formatInr(amount: Double): String {
    val rounded = "%,.0f".format(amount)
    return "₹$rounded"
}

fun formatInrExact(amount: Double): String = "₹%,.2f".format(amount)
