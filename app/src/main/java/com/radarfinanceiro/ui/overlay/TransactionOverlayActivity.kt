package com.radarfinanceiro.ui.overlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radarfinanceiro.data.database.AppDatabase
import com.radarfinanceiro.data.entity.Transaction
import com.radarfinanceiro.data.repository.TransactionRepository
import com.radarfinanceiro.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class TransactionOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transactionId = intent.getLongExtra("transaction_id", -1)
        if (transactionId == -1L) {
            finish()
            return
        }

        val db = AppDatabase.getInstance(applicationContext)
        val repository = TransactionRepository(db.transactionDao(), db.merchantMappingDao())
        val locationHelper = LocationHelper(applicationContext)

        setContent {
            MaterialTheme {
                OverlayScreen(
                    transactionId = transactionId,
                    repository = repository,
                    locationHelper = locationHelper,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayScreen(
    transactionId: Long,
    repository: TransactionRepository,
    locationHelper: LocationHelper,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var transaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedCategory by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isLuciana by remember { mutableStateOf(false) }
    var locationText by remember { mutableStateOf("Obtendo localizacao...") }
    var saving by remember { mutableStateOf(false) }

    val categories = listOf(
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
        "PIX" to Color(0xFF2196F3),
        "Outros" to Color(0xFF9E9E9E)
    )

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    LaunchedEffect(transactionId) {
        withContext(Dispatchers.IO) {
            val t = repository.getById(transactionId)
            t?.let {
                transaction = it
                selectedCategory = it.category
                isLuciana = it.isLuciana
                note = it.note

                // Resolve endereco do GPS
                if (it.latitude != null && it.longitude != null) {
                    val address = locationHelper.getAddress(it.latitude, it.longitude)
                    locationText = address.ifEmpty { "Lat: ${it.latitude}, Lon: ${it.longitude}" }
                } else {
                    locationText = "GPS nao disponivel"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        transaction?.let { t ->
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: valor e estabelecimento
                    Text(
                        text = currencyFormat.format(t.amount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (t.amount < 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = t.merchantName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Info: data, hora, cartao
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${t.date} ${t.time}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (t.cardLast4.isNotEmpty()) {
                            Text(
                                text = "Cartao ****${t.cardLast4}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (t.isInstallment) {
                            Text(
                                text = t.installmentInfo,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }

                    // Localizacao
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = locationText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Divider()

                    // Toggle: Meu / Luciana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isLuciana,
                            onClick = { isLuciana = false },
                            label = { Text("Meu gasto") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isLuciana,
                            onClick = { isLuciana = true },
                            label = { Text("Luciana") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Grid de categorias
                    Text(
                        text = "Categoria",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(180.dp)
                    ) {
                        items(categories) { (name, color) ->
                            val isSelected = name == selectedCategory
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = name },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                                border = if (isSelected)
                                    ButtonDefaults.outlinedButtonBorder
                                else null
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    // Campo de nota
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("O que voce comprou?") },
                        placeholder = { Text("Ex: pao e leite, presente da Maria...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        singleLine = false
                    )

                    // Botoes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Depois")
                        }

                        Button(
                            onClick = {
                                saving = true
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val updated = t.copy(
                                            category = if (isLuciana) "Luciana" else selectedCategory,
                                            note = note,
                                            isLuciana = isLuciana,
                                            confirmed = true,
                                            locationAddress = locationText
                                        )
                                        repository.update(updated)

                                        // Aprende a classificacao
                                        if (!isLuciana) {
                                            repository.learnCategory(
                                                t.merchantClean.ifEmpty { t.merchantName },
                                                selectedCategory,
                                                note
                                            )
                                        }
                                    }
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !saving
                        ) {
                            if (saving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Confirmar")
                            }
                        }
                    }
                }
            }
        } ?: run {
            CircularProgressIndicator(color = Color.White)
        }
    }
}
