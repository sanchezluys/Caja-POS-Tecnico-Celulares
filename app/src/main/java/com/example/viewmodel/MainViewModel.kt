package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.InventoryPart
import com.example.data.model.OrderItemPart
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig
import com.example.data.repository.AppRepository
import com.example.util.CsvBackupHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthlyReport(
    val month: Int,
    val year: Int,
    val totalOrders: Int = 0,
    val completedOrders: Int = 0,
    val successfulRepairs: Int = 0,
    val failedRepairs: Int = 0,
    val warrantyOrders: Int = 0,
    val successRatePercent: Double = 0.0,
    val grossRevenue: Double = 0.0,
    val partsWorkshopCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val avgProfitPerRepair: Double = 0.0,
    val monthOrders: List<RepairOrder> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = AppRepository(
            configDao = db.workshopConfigDao(),
            orderDao = db.repairOrderDao(),
            orderPartDao = db.orderItemPartDao(),
            inventoryDao = db.inventoryPartDao()
        )
    }

    val config: StateFlow<WorkshopConfig?> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allOrders: StateFlow<List<RepairOrder>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventory: StateFlow<List<InventoryPart>> = repository.allInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and filter state for orders
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("TODOS")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    val filteredOrders: StateFlow<List<RepairOrder>> = combine(
        allOrders,
        _searchQuery,
        _statusFilter
    ) { orders, query, filter ->
        orders.filter { order ->
            val matchesQuery = query.isBlank() ||
                    order.orderNumber.contains(query, ignoreCase = true) ||
                    order.clientName.contains(query, ignoreCase = true) ||
                    order.clientPhone.contains(query, ignoreCase = true) ||
                    order.deviceBrand.contains(query, ignoreCase = true) ||
                    order.deviceModel.contains(query, ignoreCase = true) ||
                    order.reportedIssue.contains(query, ignoreCase = true) ||
                    order.imeiOrSerial.contains(query, ignoreCase = true) ||
                    order.imei2.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "TODOS" -> true
                "GARANTIAS" -> order.isWarrantyOrder
                else -> order.status == filter
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected order for detail view
    private val _selectedOrderId = MutableStateFlow<Long?>(null)
    val selectedOrderId: StateFlow<Long?> = _selectedOrderId.asStateFlow()

    private val _orderParts = MutableStateFlow<List<OrderItemPart>>(emptyList())
    val orderParts: StateFlow<List<OrderItemPart>> = _orderParts.asStateFlow()

    private val _warrantyOrdersForSelected = MutableStateFlow<List<RepairOrder>>(emptyList())
    val warrantyOrdersForSelected: StateFlow<List<RepairOrder>> = _warrantyOrdersForSelected.asStateFlow()

    // Monthly Report Selection
    private val calendar = Calendar.getInstance()
    private val _reportMonth = MutableStateFlow(calendar.get(Calendar.MONTH) + 1)
    val reportMonth: StateFlow<Int> = _reportMonth.asStateFlow()

    private val _reportYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val reportYear: StateFlow<Int> = _reportYear.asStateFlow()

    val monthlyReport: StateFlow<MonthlyReport> = combine(
        allOrders,
        _reportMonth,
        _reportYear
    ) { orders, month, year ->
        calculateMonthlyReport(orders, month, year)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyReport(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))

    fun setReportPeriod(month: Int, year: Int) {
        _reportMonth.value = month
        _reportYear.value = year
    }

    private fun calculateMonthlyReport(orders: List<RepairOrder>, month: Int, year: Int): MonthlyReport {
        val cal = Calendar.getInstance()
        val monthOrders = orders.filter { order ->
            val timestamp = if (order.deliveredAt != null && order.deliveredAt > 0) order.deliveredAt else order.createdAt
            cal.timeInMillis = timestamp
            val orderMonth = cal.get(Calendar.MONTH) + 1
            val orderYear = cal.get(Calendar.YEAR)
            orderMonth == month && orderYear == year
        }

        val total = monthOrders.size
        val completed = monthOrders.count { it.status == OrderStatus.ENTREGADO.name || it.status == OrderStatus.LISTO_ENTREGA.name || it.status == OrderStatus.NO_REPARADO.name }
        val successful = monthOrders.count { it.repairOutcome == "EXITOSA" || (it.status == OrderStatus.ENTREGADO.name && it.repairOutcome != "NO_EXITOSA" && it.repairOutcome != "CANCELADA") }
        val failed = monthOrders.count { it.repairOutcome == "NO_EXITOSA" || it.status == OrderStatus.NO_REPARADO.name }
        val warranty = monthOrders.count { it.isWarrantyOrder }

        val evaluatedRepairs = (successful + failed)
        val successRate = if (evaluatedRepairs > 0) (successful.toDouble() / evaluatedRepairs.toDouble()) * 100.0 else 0.0

        val grossRev = monthOrders
            .filter { it.status == OrderStatus.ENTREGADO.name || it.isClosed }
            .sumOf { if (it.finalAmountPaid > 0) it.finalAmountPaid else it.finalTotal }

        val partsCost = monthOrders
            .filter { it.status == OrderStatus.ENTREGADO.name || it.isClosed }
            .sumOf { it.partsTotalCost }

        val netProfit = grossRev - partsCost
        val deliveredCount = monthOrders.count { it.status == OrderStatus.ENTREGADO.name || it.isClosed }
        val avgProfit = if (deliveredCount > 0) netProfit / deliveredCount else 0.0

        return MonthlyReport(
            month = month,
            year = year,
            totalOrders = total,
            completedOrders = completed,
            successfulRepairs = successful,
            failedRepairs = failed,
            warrantyOrders = warranty,
            successRatePercent = successRate,
            grossRevenue = grossRev,
            partsWorkshopCost = partsCost,
            netProfit = netProfit,
            avgProfitPerRepair = avgProfit,
            monthOrders = monthOrders
        )
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun selectOrder(orderId: Long?) {
        _selectedOrderId.value = orderId
        if (orderId != null) {
            viewModelScope.launch {
                repository.getPartsForOrder(orderId).collect { parts ->
                    _orderParts.value = parts
                }
            }
            viewModelScope.launch {
                repository.getWarrantyOrdersFor(orderId).collect { warrantyList ->
                    _warrantyOrdersForSelected.value = warrantyList
                }
            }
        } else {
            _orderParts.value = emptyList()
            _warrantyOrdersForSelected.value = emptyList()
        }
    }

    fun saveInitialConfig(config: WorkshopConfig, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveConfig(config.copy(isConfigured = true, configuredAt = System.currentTimeMillis()))
            onDone()
        }
    }

    fun createOrder(
        clientName: String,
        clientPhone: String,
        deviceBrand: String,
        deviceModel: String,
        imeiOrSerial: String,
        imei2: String = "",
        reportedIssue: String,
        deviceConditionNotes: String,
        budgetEstimated: Double,
        photoUri: String?,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val order = RepairOrder(
                clientName = clientName.trim(),
                clientPhone = clientPhone.trim(),
                deviceBrand = deviceBrand.trim(),
                deviceModel = deviceModel.trim(),
                imeiOrSerial = imeiOrSerial.trim(),
                imei2 = imei2.trim(),
                reportedIssue = reportedIssue.trim(),
                deviceConditionNotes = deviceConditionNotes.trim(),
                budgetEstimated = budgetEstimated,
                finalTotal = budgetEstimated,
                photoUri = photoUri,
                createdAt = System.currentTimeMillis(),
                status = OrderStatus.INGRESADO.name
            )
            val id = repository.createNewOrder(order)
            onCreated(id)
        }
    }

    fun updateOrder(order: RepairOrder) {
        viewModelScope.launch {
            repository.updateOrder(order)
        }
    }

    fun updateOrderStatusAndDiagnosis(
        orderId: Long,
        status: String,
        outcome: String?,
        diagnosis: String,
        laborCost: Double,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = repository.getOrderByIdDirect(orderId) ?: return@launch
            val updated = current.copy(
                status = status,
                repairOutcome = outcome,
                repairDiagnosis = diagnosis,
                laborCost = laborCost
            )
            repository.updateOrder(updated)
            repository.recalculateOrderFinancials(orderId)
            onComplete()
        }
    }

    fun addPartToOrder(
        orderId: Long,
        partName: String,
        quantity: Int,
        unitCost: Double,
        unitPrice: Double,
        inventoryPartId: Long?
    ) {
        viewModelScope.launch {
            val part = OrderItemPart(
                orderId = orderId,
                partId = inventoryPartId,
                partName = partName,
                quantity = quantity,
                unitCost = unitCost,
                unitPrice = unitPrice
            )
            repository.addPartToOrder(part)
        }
    }

    fun removePartFromOrder(part: OrderItemPart) {
        viewModelScope.launch {
            repository.removePartFromOrder(part)
        }
    }

    fun deliverAndCloseOrder(
        orderId: Long,
        paymentMethod: String,
        amountPaid: Double,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.deliverAndCloseOrder(orderId, paymentMethod, amountPaid)
            onDone()
        }
    }

    fun createWarrantyOrder(
        parentOrder: RepairOrder,
        reportedIssue: String,
        warrantyCost: Double,
        conditionNotes: String,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.createWarrantyOrder(
                parentOrder = parentOrder,
                reportedIssue = reportedIssue,
                warrantyCost = warrantyCost,
                conditionNotes = conditionNotes
            )
            onCreated(id)
        }
    }

    fun deleteOrder(orderId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
            if (_selectedOrderId.value == orderId) {
                selectOrder(null)
            }
            onDone()
        }
    }

    // Inventory operations
    fun addInventoryPart(part: InventoryPart, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveInventoryPart(part)
            onDone()
        }
    }

    fun updateInventoryPart(part: InventoryPart, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateInventoryPart(part)
            onDone()
        }
    }

    fun deleteInventoryPart(partId: Long) {
        viewModelScope.launch {
            repository.deleteInventoryPart(partId)
        }
    }

    fun addStock(partId: Long, qty: Int) {
        viewModelScope.launch {
            repository.addStock(partId, qty)
        }
    }

    // Export CSV
    fun exportBackupCsv(context: Context, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val cfg = repository.getConfigDirect()
                val ords = repository.getAllOrdersDirect()
                val prts = repository.getAllOrderPartsDirect()
                val inv = repository.getAllInventoryDirect()
                val csvContent = CsvBackupHelper.exportToCsvContent(cfg, ords, prts, inv)
                val uri = CsvBackupHelper.createBackupFileUri(context, csvContent)
                onResult(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    // Import CSV
    fun importBackupCsv(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val content = CsvBackupHelper.readTextFromUri(context, uri)
                if (content.isBlank()) {
                    onResult(false, "El archivo seleccionado está vacío.")
                    return@launch
                }
                val parsed = CsvBackupHelper.parseCsvContent(content)
                repository.restoreDatabase(
                    config = parsed.config,
                    orders = parsed.orders,
                    parts = parsed.orderParts,
                    inventory = parsed.inventory
                )
                onResult(true, "Restauración exitosa: ${parsed.orders.size} órdenes, ${parsed.inventory.size} repuestos en inventario.")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Error al procesar el archivo CSV: ${e.message}")
            }
        }
    }

    // Factory Reset / Wipe Data
    fun factoryReset(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.wipeAllData()
            _selectedOrderId.value = null
            _orderParts.value = emptyList()
            onComplete()
        }
    }
}
