package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DateUtils
import com.example.data.FinanceTransactionEntity
import com.example.data.SavingsGoalEntity
import com.example.data.TransactionType
import com.example.domain.FinanceEngine
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.GlassCard
import com.example.ui.components.LifeOsBadgeIcon
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel
) {
    val context = LocalContext.current
    val finance = state.financeSummary
    var filterType by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE
    var searchQuery by remember { mutableStateOf("") }
    var showAddTxDialog by remember { mutableStateOf<TransactionType?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var contributeGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val filteredTransactions = remember(state.transactions, filterType, searchQuery) {
        state.transactions.filter { tx ->
            val matchType = filterType == "ALL" || tx.type == filterType
            val matchQuery = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true)
            matchType && matchQuery
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("money_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LifeOsBadgeIcon(type = LifeOsBadgeType.FINANCE, size = 44.dp)
                        Column {
                            Text("Money Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Real-time balance, budgets & savings goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val csv = viewModel.generateFinanceCsv()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "LIFE OS Finance Export")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, "Export Finance CSV")) }
                        },
                        modifier = Modifier.testTag("money_export_csv_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Export Finance CSV")
                    }
                }
            }

            // Hero Available Balance Card (Exact Automatic Formula)
            item {
                GlassCard(accentTint = MaterialTheme.colorScheme.primary) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Available Balance", style = MaterialTheme.typography.titleMedium)
                            }
                            TextButton(
                                onClick = { showBudgetDialog = true },
                                modifier = Modifier.testTag("money_edit_balance_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Setup")
                            }
                        }
                        Text(
                            text = FinanceEngine.formatInr(finance.availableBalance),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (finance.availableBalance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("money_available_balance")
                        )
                        Text(
                            text = "Initial ${FinanceEngine.formatInr(finance.initialBalance)} + Income ${FinanceEngine.formatInr(finance.totalIncome)} − Expenses ${FinanceEngine.formatInr(finance.totalExpenses)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showAddTxDialog = TransactionType.EXPENSE },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("money_add_expense_button")
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Expense")
                            }
                            FilledTonalButton(
                                onClick = { showAddTxDialog = TransactionType.INCOME },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("money_add_income_button")
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Income")
                            }
                        }
                    }
                }
            }

            // Alerts (if budget or daily limit exceeded)
            if (finance.alerts.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            finance.alerts.forEach { alert ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Text(alert, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }

            // Spending & Budget Metrics Row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassCard(cornerRadius = 18.dp, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Today's Spend", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceEngine.formatInr(finance.todayExpense), fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Limit ${FinanceEngine.formatInr(finance.dailySpendLimit)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    GlassCard(cornerRadius = 18.dp, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Monthly Budget", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(FinanceEngine.formatInr(finance.budgetRemaining), fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (finance.budgetRemaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                            Text("Spent ${FinanceEngine.formatInr(finance.monthlyExpense)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Category Breakdown
            if (finance.expenseByCategory.isNotEmpty()) {
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Monthly Expense Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val maxCat = finance.expenseByCategory.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                            finance.expenseByCategory.take(6).forEach { (cat, total) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(cat, style = MaterialTheme.typography.bodyMedium)
                                        Text(FinanceEngine.formatInr(total), fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelMedium)
                                    }
                                    LinearProgressIndicator(
                                        progress = { (total / maxCat).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Savings Goals Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Text("Savings Goals (${state.savingsGoals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(
                        onClick = { showAddSavingsDialog = true },
                        modifier = Modifier.testTag("money_add_savings_goal_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Goal")
                    }
                }
            }

            if (state.savingsGoals.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "No savings goals yet. Create a goal for an emergency fund, laptop, travel, or education.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(state.savingsGoals, key = { "sg_${it.id}" }) { goal ->
                    val pct = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
                    GlassCard(cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${FinanceEngine.formatInr(goal.savedAmount)} / ${FinanceEngine.formatInr(goal.targetAmount)} • ${(pct * 100).toInt()}%",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Row {
                                    OutlinedButton(onClick = { contributeGoal = goal }) { Text("+ Save") }
                                    IconButton(onClick = { viewModel.deleteSavingsGoal(goal.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete savings goal", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(999.dp))
                            )
                            Text(
                                text = "Remaining: ${FinanceEngine.formatInr(remaining)} • Target date: ${goal.targetDateIso}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Transaction History + Search/Filter
            item {
                Spacer(Modifier.height(4.dp))
                Text("Transactions (${filteredTransactions.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search category or note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                val filterKeys = listOf("ALL", TransactionType.EXPENSE.name, TransactionType.INCOME.name)
                AppleSegmentedControl(
                    options = listOf("All", "Expenses", "Income"),
                    selectedIndex = filterKeys.indexOf(filterType).coerceAtLeast(0),
                    onSelect = { idx -> filterType = filterKeys[idx] }
                )
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "No transactions recorded yet. Add an expense or income entry above to see your live balance update.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(filteredTransactions.take(40), key = { it.id }) { tx ->
                    val isIncome = tx.type == TransactionType.INCOME.name
                    GlassCard(cornerRadius = 16.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = listOfNotNull(
                                        tx.dateIso,
                                        tx.note.takeIf { it.isNotBlank() },
                                        tx.locationTag.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = (if (isIncome) "+" else "-") + FinanceEngine.formatInr(tx.amount),
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                                IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete transaction", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Income/Expense Dialog
    showAddTxDialog?.let { txType ->
        var amountText by remember { mutableStateOf("") }
        val presets = if (txType == TransactionType.INCOME) FinanceEngine.defaultIncomeCategories else FinanceEngine.defaultExpenseCategories
        var selectedCategory by remember { mutableStateOf(presets.first()) }
        var customCat by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var locationTag by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTxDialog = null },
            title = { Text(if (txType == TransactionType.INCOME) "Add Income" else "Add Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        presets.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    if (selectedCategory == "Custom" || selectedCategory == "Other Income") {
                        OutlinedTextField(
                            value = customCat,
                            onValueChange = { customCat = it },
                            label = { Text("Custom category name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = locationTag,
                        onValueChange = { locationTag = it },
                        label = { Text("Location tag (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        val finalCat = if ((selectedCategory == "Custom" || selectedCategory == "Other Income") && customCat.isNotBlank()) {
                            customCat.trim()
                        } else selectedCategory
                        viewModel.saveTransaction(
                            FinanceTransactionEntity(
                                userId = state.userId,
                                type = txType.name,
                                amount = amountText.toDoubleOrNull() ?: 0.0,
                                category = finalCat,
                                note = note.trim(),
                                dateIso = state.selectedDateIso,
                                locationTag = locationTag.trim()
                            )
                        )
                        showAddTxDialog = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddTxDialog = null }) { Text("Cancel") } }
        )
    }

    // Initial Balance & Budget Setup Dialog
    if (showBudgetDialog) {
        var initBal by remember { mutableStateOf(state.profile.initialBalance.toLong().toString()) }
        var monthlyBudget by remember { mutableStateOf(state.profile.monthlyBudget.toLong().toString()) }
        var dailyLimit by remember { mutableStateOf(state.profile.dailySpendLimit.toLong().toString()) }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Balance & Budget Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initBal,
                        onValueChange = { initBal = it },
                        label = { Text("Initial Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = monthlyBudget,
                        onValueChange = { monthlyBudget = it },
                        label = { Text("Monthly Budget Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dailyLimit,
                        onValueChange = { dailyLimit = it },
                        label = { Text("Daily Spending Limit (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProfile {
                        it.copy(
                            initialBalance = initBal.toDoubleOrNull() ?: it.initialBalance,
                            monthlyBudget = monthlyBudget.toDoubleOrNull() ?: it.monthlyBudget,
                            dailySpendLimit = dailyLimit.toDoubleOrNull() ?: it.dailySpendLimit
                        )
                    }
                    showBudgetDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") } }
        )
    }

    // New Savings Goal Dialog
    if (showAddSavingsDialog) {
        var title by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        var initialSaved by remember { mutableStateOf("0") }
        var targetDate by remember { mutableStateOf(DateUtils.offsetDateIso(DateUtils.todayIso(), 90)) }

        AlertDialog(
            onDismissRequest = { showAddSavingsDialog = false },
            title = { Text("New Savings Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal name (e.g. Emergency Fund)") }, singleLine = true)
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = initialSaved, onValueChange = { initialSaved = it }, label = { Text("Saved So Far (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = targetDate, onValueChange = { targetDate = it }, label = { Text("Target Date (YYYY-MM-DD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        viewModel.saveSavingsGoal(
                            SavingsGoalEntity(
                                userId = state.userId,
                                title = title.trim(),
                                targetAmount = target.toDoubleOrNull() ?: 1000.0,
                                savedAmount = initialSaved.toDoubleOrNull() ?: 0.0,
                                targetDateIso = targetDate.trim().take(10)
                            )
                        )
                        showAddSavingsDialog = false
                    }
                ) { Text("Create Goal") }
            },
            dismissButton = { TextButton(onClick = { showAddSavingsDialog = false }) { Text("Cancel") } }
        )
    }

    // Add Contribution Dialog
    contributeGoal?.let { goal ->
        var addAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { contributeGoal = null },
            title = { Text("Add Savings to ${goal.title}") },
            text = {
                OutlinedTextField(
                    value = addAmount,
                    onValueChange = { addAmount = it },
                    label = { Text("Amount to add (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = (addAmount.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        viewModel.addContributionToSavingsGoal(goal, addAmount.toDoubleOrNull() ?: 0.0)
                        contributeGoal = null
                    }
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { contributeGoal = null }) { Text("Cancel") } }
        )
    }
}
