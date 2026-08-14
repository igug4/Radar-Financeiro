package com.radarfinanceiro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radarfinanceiro.data.dao.CategorySummary
import com.radarfinanceiro.data.entity.Transaction
import java.text.NumberFormat
import java.util.Locale

val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

val categoryColors = mapOf(
    "Alimentacao" to Color(0xFF4CAF50),
    "Combustivel" to Color(0xFFFF9800),
    "Saude" to Color(0xFFF44336),
    "Restaurante" to Color(0xFFE91E63),
    "Vestuario" to Color(0xFF9C27B0),
    "Educacao" to Color(0xFF3F51B5),
    "Casa" to Color(0xFF795548),
    "Transporte" to Color(0xFF607D8B),
    "Viagem" to Color(0xFF00BCD4),
    "Servicos" to Color(0xFF009688),
    "Lazer" to Color(0xFFFF5722),
    "Compras Online" to Color(0xFF673AB7),
    "Luciana" to Color(0xFFEC407A),
    "PIX" to Color(0xFF2196F3),
    "Outros" to Color(0xFF9E9E9E)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val categorySummary by viewModel.categorySummary.collectAsState(initial = emptyList())
    val myTotal by viewModel.myTotal.collectAsState(initial = 0.0)
    val lucianaTotal by viewModel.lucianaTotal.collectAsState(initial = 0.0)
    val pendingCount by viewModel.pendingCount.collectAsState(initial = 0)

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Radar Financeiro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                ),
                actions = {
                    if (pendingCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text("$pendingCount") }
                            }
                        ) {
                            IconButton(onClick = { /* TODO: tela de pendentes */ }) {
                                Icon(Icons.Default.Notifications, "Pendentes", tint = Color.White)
                            }
                        }
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "Buscar", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: lancamento manual */ },
                containerColor = Color(0xFF1B5E20)
            ) {
                Icon(Icons.Default.Add, "Adicionar", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Busca
            if (showSearch) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.search(it)
                        },
                        label = { Text("Buscar por estabelecimento ou nota") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = ""; viewModel.search("") }) {
                                    Icon(Icons.Default.Clear, "Limpar")
                                }
                            }
                        },
                        singleLine = true
                    )
                }

                if (searchQuery.length >= 2) {
                    items(searchResults) { t ->
                        TransactionCard(t) { selectedTransaction = it }
                    }
                    return@LazyColumn
                }
            }

            // Navegacao de mes
            item {
                MonthSelector(
                    month = currentMonth,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
            }

            // Resumo
            item {
                SummaryCard(
                    myTotal = myTotal ?: 0.0,
                    lucianaTotal = lucianaTotal ?: 0.0
                )
            }

            // Resumo por categoria
            if (categorySummary.isNotEmpty()) {
                item {
                    Text(
                        "Por categoria",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(categorySummary) { summary ->
                    CategoryBar(summary, (myTotal ?: 0.0) + (lucianaTotal ?: 0.0))
                }
            }

            // Lista de transacoes
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Lancamentos (${transactions.size})",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(transactions) { t ->
                TransactionCard(t) { selectedTransaction = it }
            }

            // Espaco para o FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Dialog de detalhes/edicao
    selectedTransaction?.let { t ->
        TransactionDetailDialog(
            transaction = t,
            onDismiss = { selectedTransaction = null },
            onSave = { updated ->
                viewModel.updateTransaction(updated)
                selectedTransaction = null
            },
            onDelete = {
                viewModel.deleteTransaction(t)
                selectedTransaction = null
            }
        )
    }
}

@Composable
fun MonthSelector(month: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    val monthNames = mapOf(
        "01" to "Janeiro", "02" to "Fevereiro", "03" to "Marco",
        "04" to "Abril", "05" to "Maio", "06" to "Junho",
        "07" to "Julho", "08" to "Agosto", "09" to "Setembro",
        "10" to "Outubro", "11" to "Novembro", "12" to "Dezembro"
    )
    val parts = month.split("/")
    val displayName = "${monthNames[parts[0]] ?: parts[0]} ${parts[1]}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, "Mes anterior")
        }
        Text(
            text = displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, "Proximo mes")
        }
    }
}

@Composable
fun SummaryCard(myTotal: Double, lucianaTotal: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total do mes", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(
                currencyFormat.format(myTotal + lucianaTotal),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Meus gastos", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(currencyFormat.format(myTotal), color = Color.White, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Luciana", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(currencyFormat.format(lucianaTotal), color = Color(0xFFEC407A), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CategoryBar(summary: CategorySummary, total: Double) {
    val color = categoryColors[summary.category] ?: Color.Gray
    val percentage = if (total > 0) (summary.total / total * 100) else 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            summary.category,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp
        )
        Text(
            "${summary.count}x",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            currencyFormat.format(summary.total),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            String.format("%.0f%%", percentage),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TransactionCard(transaction: Transaction, onClick: (Transaction) -> Unit) {
    val color = categoryColors[transaction.category] ?: Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (transaction.confirmed)
                MaterialTheme.colorScheme.surface
            else
                Color(0xFFFFF3E0) // Laranja claro para pendentes
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de categoria
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    transaction.category.take(2).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.merchantClean.ifEmpty { transaction.merchantName },
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (transaction.isLuciana) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEC407A).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "L",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEC407A),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (transaction.note.isNotEmpty()) {
                    Text(
                        transaction.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    Text(
                        "${transaction.date} ${transaction.time}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.locationAddress.isNotEmpty()) {
                        Text(
                            " - ${transaction.locationAddress}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currencyFormat.format(transaction.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (transaction.amount < 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                val sourceLabel = when (transaction.source) {
                    "itau" -> "Itau"
                    "bradesco_sms" -> "Amazon"
                    "google_wallet" -> "GWallet"
                    "pix" -> "PIX"
                    "manual" -> "Manual"
                    else -> transaction.source
                }
                Text(sourceLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: () -> Unit
) {
    var category by remember { mutableStateOf(transaction.category) }
    var note by remember { mutableStateOf(transaction.note) }
    var isLuciana by remember { mutableStateOf(transaction.isLuciana) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                currencyFormat.format(transaction.amount),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(transaction.merchantName, fontWeight = FontWeight.Medium)
                Text(
                    "${transaction.date} ${transaction.time}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (transaction.locationAddress.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(transaction.locationAddress, fontSize = 12.sp)
                    }
                }

                Divider()

                // Toggle meu/Luciana
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isLuciana,
                        onClick = { isLuciana = false },
                        label = { Text("Meu") }
                    )
                    FilterChip(
                        selected = isLuciana,
                        onClick = { isLuciana = true },
                        label = { Text("Luciana") }
                    )
                }

                // Categoria (dropdown simples)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categoryColors.keys.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    transaction.copy(
                        category = if (isLuciana) "Luciana" else category,
                        note = note,
                        isLuciana = isLuciana,
                        confirmed = true
                    )
                )
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("Excluir")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}
