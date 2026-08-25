package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RepairOrder
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.NewOrderDialog
import com.example.ui.screens.OnboardingSetupScreen
import com.example.ui.screens.OrderDetailScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.viewmodel.MainViewModel

enum class MainTab(val title: String) {
    ORDERS("Órdenes"),
    INVENTORY("Inventario"),
    REPORTS("Reportes"),
    SETTINGS("Ajustes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val filteredOrders by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val selectedOrderId by viewModel.selectedOrderId.collectAsStateWithLifecycle()
    val orderParts by viewModel.orderParts.collectAsStateWithLifecycle()
    val warrantyOrders by viewModel.warrantyOrdersForSelected.collectAsStateWithLifecycle()
    val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.ORDERS) }
    var showNewOrderDialog by remember { mutableStateOf(false) }

    // 1. Initial Setup / Onboarding if workshop not yet configured
    if (config == null || !config!!.isConfigured) {
        OnboardingSetupScreen(
            onSaveConfig = { newConfig ->
                viewModel.saveInitialConfig(newConfig) {
                    Toast.makeText(context, "¡Taller configurado exitosamente!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = modifier
        )
        return
    }

    val currentConfig = config!!

    // 2. Order Detail Screen when an order is selected
    if (selectedOrderId != null) {
        val selectedOrder = allOrders.find { it.id == selectedOrderId }
        if (selectedOrder != null) {
            OrderDetailScreen(
                order = selectedOrder,
                parts = orderParts,
                warrantyOrders = warrantyOrders,
                inventory = inventory,
                config = currentConfig,
                onBack = { viewModel.selectOrder(null) },
                onUpdateStatusAndDiagnosis = { status, outcome, diagnosis, laborCost ->
                    viewModel.updateOrderStatusAndDiagnosis(selectedOrder.id, status, outcome, diagnosis, laborCost) {
                        Toast.makeText(context, "Estado de orden actualizado", Toast.LENGTH_SHORT).show()
                    }
                },
                onAddPart = { name, qty, cost, price, invId ->
                    viewModel.addPartToOrder(selectedOrder.id, name, qty, cost, price, invId)
                },
                onRemovePart = { part ->
                    viewModel.removePartFromOrder(part)
                },
                onDeliverAndClose = { paymentMethod, amountPaid ->
                    viewModel.deliverAndCloseOrder(selectedOrder.id, paymentMethod, amountPaid) {
                        Toast.makeText(context, "Orden entregada y cerrada exitosamente", Toast.LENGTH_SHORT).show()
                    }
                },
                onCreateWarrantyOrder = { issue, cost, notes ->
                    viewModel.createWarrantyOrder(selectedOrder, issue, cost, notes) { warrantyId ->
                        Toast.makeText(context, "Orden de garantía creada", Toast.LENGTH_SHORT).show()
                        viewModel.selectOrder(warrantyId)
                    }
                },
                onDeleteOrder = {
                    viewModel.deleteOrder(selectedOrder.id) {
                        Toast.makeText(context, "Orden eliminada", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            return
        }
    }

    // 3. Main Application with Bottom Navigation
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentTab) {
                                MainTab.ORDERS -> currentConfig.workshopName.ifBlank { "Servicio Técnico" }
                                MainTab.INVENTORY -> "Inventario de Repuestos"
                                MainTab.REPORTS -> "Reporte Mensual y Ganancias"
                                MainTab.SETTINGS -> "Ajustes del Taller"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (currentConfig.technicianName.isNotBlank()) {
                            Text(
                                text = "Técnico: ${currentConfig.technicianName}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Orders Tab
                NavigationBarItem(
                    selected = currentTab == MainTab.ORDERS,
                    onClick = { currentTab = MainTab.ORDERS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.ORDERS) Icons.Filled.PhoneAndroid else Icons.Outlined.PhoneAndroid,
                            contentDescription = "Órdenes",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Órdenes", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.ORDERS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_orders")
                )

                // Inventory Tab
                NavigationBarItem(
                    selected = currentTab == MainTab.INVENTORY,
                    onClick = { currentTab = MainTab.INVENTORY },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.INVENTORY) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Inventario",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Inventario", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.INVENTORY) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_inventory")
                )

                // Reports Tab
                NavigationBarItem(
                    selected = currentTab == MainTab.REPORTS,
                    onClick = { currentTab = MainTab.REPORTS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.REPORTS) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                            contentDescription = "Reportes",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Reportes", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.REPORTS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_reports")
                )

                // Settings Tab
                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Ajustes",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Ajustes", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                MainTab.ORDERS -> {
                    OrdersScreen(
                        orders = filteredOrders,
                        searchQuery = searchQuery,
                        statusFilter = statusFilter,
                        config = currentConfig,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterChange = { viewModel.setStatusFilter(it) },
                        onSelectOrder = { order -> viewModel.selectOrder(order.id) },
                        onNewOrderClick = { showNewOrderDialog = true }
                    )
                }
                MainTab.INVENTORY -> {
                    InventoryScreen(
                        inventory = inventory,
                        config = currentConfig,
                        onAddPart = { part ->
                            viewModel.addInventoryPart(part) {
                                Toast.makeText(context, "Repuesto agregado al inventario", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onUpdatePart = { part ->
                            viewModel.updateInventoryPart(part) {
                                Toast.makeText(context, "Repuesto actualizado", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeletePart = { id ->
                            viewModel.deleteInventoryPart(id)
                            Toast.makeText(context, "Repuesto eliminado", Toast.LENGTH_SHORT).show()
                        },
                        onAddStock = { id, qty ->
                            viewModel.addStock(id, qty)
                        }
                    )
                }
                MainTab.REPORTS -> {
                    ReportsScreen(
                        report = monthlyReport,
                        config = currentConfig,
                        onPeriodChange = { m, y -> viewModel.setReportPeriod(m, y) },
                        onSelectOrder = { order -> viewModel.selectOrder(order.id) }
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        config = currentConfig,
                        onExportBackup = { ctx, callback ->
                            viewModel.exportBackupCsv(ctx, callback)
                        },
                        onImportBackup = { ctx, uri, callback ->
                            viewModel.importBackupCsv(ctx, uri, callback)
                        },
                        onFactoryReset = { callback ->
                            viewModel.factoryReset(callback)
                        }
                    )
                }
            }
        }
    }

    // New Order Intake Dialog
    if (showNewOrderDialog) {
        NewOrderDialog(
            config = currentConfig,
            onDismiss = { showNewOrderDialog = false },
            onSubmit = { name, phone, brand, model, imei, imei2, issue, condition, budget, photoUri ->
                viewModel.createOrder(
                    clientName = name,
                    clientPhone = phone,
                    deviceBrand = brand,
                    deviceModel = model,
                    imeiOrSerial = imei,
                    imei2 = imei2,
                    reportedIssue = issue,
                    deviceConditionNotes = condition,
                    budgetEstimated = budget,
                    photoUri = photoUri
                ) { newOrderId ->
                    showNewOrderDialog = false
                    Toast.makeText(context, "Orden creada exitosamente", Toast.LENGTH_SHORT).show()
                    viewModel.selectOrder(newOrderId)
                }
            }
        )
    }
}
