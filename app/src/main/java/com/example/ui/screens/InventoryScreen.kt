package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InventoryPart
import com.example.data.model.WorkshopConfig
import com.example.util.CostValidator
import com.example.util.CurrencyFormatHelper
import java.util.Locale

@Composable
fun InventoryScreen(
    inventory: List<InventoryPart>,
    config: WorkshopConfig,
    onAddPart: (InventoryPart) -> Unit,
    onUpdatePart: (InventoryPart) -> Unit,
    onDeletePart: (Long) -> Unit,
    onAddStock: (partId: Long, qty: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var partToEdit by remember { mutableStateOf<InventoryPart?>(null) }
    var partToDelete by remember { mutableStateOf<InventoryPart?>(null) }

    val filteredInventory = remember(inventory, searchQuery) {
        if (searchQuery.isBlank()) inventory
        else inventory.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.brandCompatibility.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalItems = inventory.size
    val totalStockUnits = inventory.sumOf { it.stockQuantity }
    val lowStockCount = inventory.count { it.stockQuantity <= it.minStockAlert }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Inventory Top Metric Cards (Natural Tones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total Parts (Blue tone)
                Surface(
                    color = Color(0xFFE1F1FF),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCE5FF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Repuestos", style = MaterialTheme.typography.labelSmall, color = Color(0xFF003258))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "$totalItems tipos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF001D35))
                    }
                }

                // Card 2: Units (Sage tone)
                Surface(
                    color = Color(0xFFE9F0E6),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1E1D1)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Unidades", style = MaterialTheme.typography.labelSmall, color = Color(0xFF175222))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "$totalStockUnits uds", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF072100))
                    }
                }

                // Card 3: Low Stock Warning (Warm Amber or Sage tone)
                Surface(
                    color = if (lowStockCount > 0) Color(0xFFFFF0D6) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (lowStockCount > 0) Color(0xFFFFDDB3) else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Stock Bajo", style = MaterialTheme.typography.labelSmall, color = if (lowStockCount > 0) Color(0xFF4E2600) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "$lowStockCount alertas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (lowStockCount > 0) Color(0xFF7A3600) else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre, código o compatibilidad...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_inventory")
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredInventory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron repuestos" else "Inventario vacío",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Registre pantallas, baterías, conectores y repuestos para descontar automáticamente al cerrar órdenes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("inventory_list")
                ) {
                    items(filteredInventory, key = { it.id }) { part ->
                        InventoryCardItem(
                            part = part,
                            config = config,
                            onEdit = { partToEdit = part },
                            onDelete = { partToDelete = part },
                            onAddStock = { qty -> onAddStock(part.id, qty) }
                        )
                    }
                }
            }
        }

        // FAB to add part
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_new_inventory_part")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nuevo Repuesto")
        }
    }

    // Add Part Dialog
    if (showAddDialog) {
        InventoryPartFormDialog(
            title = "Nuevo Repuesto",
            initialPart = null,
            onDismiss = { showAddDialog = false },
            onSave = { newPart ->
                onAddPart(newPart)
                showAddDialog = false
            }
        )
    }

    // Edit Part Dialog
    if (partToEdit != null) {
        InventoryPartFormDialog(
            title = "Editar Repuesto",
            initialPart = partToEdit,
            onDismiss = { partToEdit = null },
            onSave = { updatedPart ->
                onUpdatePart(updatedPart)
                partToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (partToDelete != null) {
        AlertDialog(
            onDismissRequest = { partToDelete = null },
            title = { Text("¿Eliminar del Inventario?") },
            text = { Text("¿Desea eliminar \"${partToDelete?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        partToDelete?.let { onDeletePart(it.id) }
                        partToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { partToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun InventoryCardItem(
    part: InventoryPart,
    config: WorkshopConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddStock: (Int) -> Unit
) {
    val isLowStock = part.stockQuantity <= part.minStockAlert

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_item_${part.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Part Name, Compatibility badge, Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = part.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (part.code.isNotBlank()) {
                            Text(
                                text = "Cód: ${part.code} • ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = part.brandCompatibility,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Pricing and Stock Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Costs info
                Column {
                    Text(
                        text = "Costo Taller: ${CurrencyFormatHelper.formatCop(part.purchaseCost, config)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Precio Cliente: ${CurrencyFormatHelper.formatCop(part.salePrice, config)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Stock Counter with +/-
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Stock badge
                    Surface(
                        color = if (isLowStock) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (isLowStock) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Stock Bajo",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "${part.stockQuantity} en stock",
                                color = if (isLowStock) Color(0xFFDC2626) else Color(0xFF059669),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // +1 Quick add button
                    IconButton(
                        onClick = { onAddStock(1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Añadir 1",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryPartFormDialog(
    title: String,
    initialPart: InventoryPart?,
    onDismiss: () -> Unit,
    onSave: (InventoryPart) -> Unit
) {
    var code by remember { mutableStateOf(initialPart?.code ?: "") }
    var name by remember { mutableStateOf(initialPart?.name ?: "") }
    var brandCompatibility by remember { mutableStateOf(initialPart?.brandCompatibility ?: "Universal") }
    var purchaseCostStr by remember { mutableStateOf(initialPart?.let { if (it.purchaseCost > 0) String.format(Locale.US, "%.0f", it.purchaseCost) else "" } ?: "") }
    var salePriceStr by remember { mutableStateOf(initialPart?.let { if (it.salePrice > 0) String.format(Locale.US, "%.0f", it.salePrice) else "" } ?: "") }
    var stockStr by remember { mutableStateOf(initialPart?.stockQuantity?.toString() ?: "1") }
    var minAlertStr by remember { mutableStateOf(initialPart?.minStockAlert?.toString() ?: "2") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("Nombre del Repuesto *") },
                    placeholder = { Text("Ej: Pantalla OLED iPhone 13") },
                    singleLine = true,
                    isError = showError && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código / Ref") },
                        placeholder = { Text("SCR-IP13") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = brandCompatibility,
                        onValueChange = { brandCompatibility = it },
                        label = { Text("Compatibilidad") },
                        placeholder = { Text("Apple, Samsung...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val purchaseCostCheck = CostValidator.validate(purchaseCostStr, isRequired = false)
                    OutlinedTextField(
                        value = purchaseCostStr,
                        onValueChange = { input ->
                            purchaseCostStr = CostValidator.filterNumericInput(input)
                        },
                        label = { Text("Costo Taller (COP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !purchaseCostCheck.isValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    val salePriceCheck = CostValidator.validate(salePriceStr, isRequired = true)
                    OutlinedTextField(
                        value = salePriceStr,
                        onValueChange = { input ->
                            salePriceStr = CostValidator.filterNumericInput(input)
                        },
                        label = { Text("Precio Cliente (COP) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !salePriceCheck.isValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { input ->
                            stockStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Cantidad en Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minAlertStr,
                        onValueChange = { input ->
                            minAlertStr = input.filter { it.isDigit() }
                        },
                        label = { Text("Alerta Stock Mínimo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete los campos obligatorios y valide que los costos sean números válidos",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val costCheck = CostValidator.validate(purchaseCostStr, isRequired = false)
                            val priceCheck = CostValidator.validate(salePriceStr, isRequired = true)

                            if (name.isBlank() || !costCheck.isValid || !priceCheck.isValid) {
                                showError = true
                            } else {
                                val cost = costCheck.value
                                val price = priceCheck.value
                                val stock = stockStr.toIntOrNull() ?: 0
                                val alert = minAlertStr.toIntOrNull() ?: 2

                                val part = initialPart?.copy(
                                    code = code.trim(),
                                    name = name.trim(),
                                    brandCompatibility = brandCompatibility.trim().ifBlank { "Universal" },
                                    purchaseCost = cost,
                                    salePrice = price,
                                    stockQuantity = stock,
                                    minStockAlert = alert
                                ) ?: InventoryPart(
                                    code = code.trim(),
                                    name = name.trim(),
                                    brandCompatibility = brandCompatibility.trim().ifBlank { "Universal" },
                                    purchaseCost = cost,
                                    salePrice = price,
                                    stockQuantity = stock,
                                    minStockAlert = alert
                                )

                                onSave(part)
                            }
                        }
                    ) {
                        Text("Guardar Repuesto")
                    }
                }
            }
        }
    }
}
