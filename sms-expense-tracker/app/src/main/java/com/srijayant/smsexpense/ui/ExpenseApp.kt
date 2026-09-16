package com.srijayant.smsexpense.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srijayant.smsexpense.data.Category
import com.srijayant.smsexpense.data.Transaction
import com.srijayant.smsexpense.data.TransactionType
import com.srijayant.smsexpense.ui.theme.CategoryColors
import com.srijayant.smsexpense.ui.theme.ReceivedGreen
import com.srijayant.smsexpense.ui.theme.SpentRed
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val currencyFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
private val dayFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a")

fun formatAmount(value: Double): String = currencyFormat.format(value)

private fun categoryColor(category: Category) =
    CategoryColors[category.ordinal % CategoryColors.size]

@Composable
fun ExpenseApp(viewModel: ExpenseViewModel, initiallyGranted: Boolean) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initiallyGranted) {
        if (initiallyGranted) viewModel.onPermissionResult(true)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onPermissionResult(granted) }

    if (!state.permissionGranted) {
        PermissionScreen(onRequest = { permissionLauncher.launch(Manifest.permission.READ_SMS) })
    } else {
        ReportScreen(state = state, viewModel = viewModel)
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Track expenses from your SMS",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "This app scans bank and UPI transaction messages in your inbox to " +
                "build a monthly spending report.\n\nEverything stays on your " +
                "device — nothing is uploaded, stored elsewhere, or shared.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRequest) {
            Text("Allow SMS access")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportScreen(state: UiState, viewModel: ExpenseViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Report", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Rescan messages")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val report = state.report
        when {
            state.loading && report == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            report != null -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { MonthSelector(state, viewModel) }
                item { SummaryRow(report) }
                if (report.categoryTotals.isNotEmpty()) {
                    item { CategoryBreakdownCard(report) }
                }
                if (report.transactions.isEmpty()) {
                    item { EmptyMonthCard(state.scannedSmsCount) }
                } else {
                    item {
                        Text(
                            "Transactions (${report.transactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(report.transactions) { txn -> TransactionRow(txn) }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(state: UiState, viewModel: ExpenseViewModel) {
    val months = state.availableMonths
    val idx = months.indexOf(state.selectedMonth)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = { viewModel.previousMonth() },
            enabled = idx in 0 until months.size - 1
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            state.selectedMonth.format(monthFormat),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = { viewModel.nextMonth() },
            enabled = idx > 0
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun SummaryRow(report: MonthlyReport) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard("Spent", report.totalSpent, SpentRed, Modifier.weight(1f))
        SummaryCard("Received", report.totalReceived, ReceivedGreen, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                formatAmount(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(report: MonthlyReport) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Where it went", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(report.categoryTotals, Modifier.size(120.dp))
                Spacer(Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    report.categoryTotals.take(5).forEach { ct ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(categoryColor(ct.category), CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                ct.category.label,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            report.categoryTotals.forEach { ct ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ct.category.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${formatAmount(ct.total)} · ${(ct.share * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { ct.share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = categoryColor(ct.category),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(totals: List<CategoryTotal>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.18f
        val inset = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        var startAngle = -90f
        totals.forEach { ct ->
            val sweep = ct.share * 360f
            drawArc(
                color = CategoryColors[ct.category.ordinal % CategoryColors.size],
                startAngle = startAngle,
                sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun TransactionRow(txn: Transaction) {
    val isDebit = txn.type == TransactionType.DEBIT
    Card {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(categoryColor(txn.category).copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    txn.category.label.first().toString(),
                    color = categoryColor(txn.category),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    txn.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${txn.category.label} · " + Instant.ofEpochMilli(txn.timestampMillis)
                        .atZone(ZoneId.systemDefault()).format(dayFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (isDebit) "-" else "+") + formatAmount(txn.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDebit) SpentRed else ReceivedGreen
            )
        }
    }
}

@Composable
private fun EmptyMonthCard(scannedCount: Int) {
    Card {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No transactions found", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                "Scanned $scannedCount messages for this period. Transaction " +
                    "alerts from banks, cards and UPI apps will appear here " +
                    "automatically.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
