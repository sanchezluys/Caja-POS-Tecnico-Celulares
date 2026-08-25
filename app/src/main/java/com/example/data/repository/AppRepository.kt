package com.example.data.repository

import com.example.data.dao.InventoryPartDao
import com.example.data.dao.OrderItemPartDao
import com.example.data.dao.RepairOrderDao
import com.example.data.dao.WorkshopConfigDao
import com.example.data.model.InventoryPart
import com.example.data.model.OrderItemPart
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppRepository(
    private val configDao: WorkshopConfigDao,
    private val orderDao: RepairOrderDao,
    private val orderPartDao: OrderItemPartDao,
    private val inventoryDao: InventoryPartDao
) {
    // Config
    val config: Flow<WorkshopConfig?> = configDao.getConfig()
    suspend fun getConfigDirect(): WorkshopConfig? = configDao.getConfigDirect()
    suspend fun saveConfig(config: WorkshopConfig) = configDao.insertOrUpdate(config)

    // Orders
    val allOrders: Flow<List<RepairOrder>> = orderDao.getAllOrders()
    fun getOrderById(id: Long): Flow<RepairOrder?> = orderDao.getOrderById(id)
    suspend fun getOrderByIdDirect(id: Long): RepairOrder? = orderDao.getOrderByIdDirect(id)
    fun getWarrantyOrdersFor(parentId: Long): Flow<List<RepairOrder>> = orderDao.getWarrantyOrdersForParent(parentId)

    suspend fun createNewOrder(order: RepairOrder): Long {
        val count = orderDao.getOrdersCount() + 1
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val formattedNumber = "ORD-$year-${String.format(Locale.US, "%04d", count)}"
        val newOrder = order.copy(
            orderNumber = if (order.orderNumber.isBlank()) formattedNumber else order.orderNumber,
            createdAt = if (order.createdAt == 0L) System.currentTimeMillis() else order.createdAt
        )
        return orderDao.insertOrder(newOrder)
    }

    suspend fun updateOrder(order: RepairOrder) {
        orderDao.updateOrder(order)
    }

    suspend fun deleteOrder(orderId: Long) {
        orderPartDao.deletePartsForOrder(orderId)
        orderDao.deleteOrderById(orderId)
    }

    // Order Parts
    fun getPartsForOrder(orderId: Long): Flow<List<OrderItemPart>> = orderPartDao.getPartsForOrder(orderId)
    suspend fun getPartsForOrderDirect(orderId: Long): List<OrderItemPart> = orderPartDao.getPartsForOrderDirect(orderId)

    suspend fun addPartToOrder(part: OrderItemPart): Long {
        val id = orderPartDao.insertPart(part)
        recalculateOrderFinancials(part.orderId)
        return id
    }

    suspend fun removePartFromOrder(part: OrderItemPart) {
        orderPartDao.deletePart(part)
        recalculateOrderFinancials(part.orderId)
    }

    suspend fun recalculateOrderFinancials(orderId: Long) {
        val order = orderDao.getOrderByIdDirect(orderId) ?: return
        val parts = orderPartDao.getPartsForOrderDirect(orderId)
        val totalCost = parts.sumOf { it.unitCost * it.quantity }
        val totalPrice = parts.sumOf { it.unitPrice * it.quantity }
        val finalTotal = if (order.isWarrantyOrder) {
            order.warrantyCost
        } else {
            order.laborCost + totalPrice
        }
        val updated = order.copy(
            partsTotalCost = totalCost,
            partsTotalPrice = totalPrice,
            finalTotal = finalTotal
        )
        orderDao.updateOrder(updated)
    }

    // Complete / Deliver Order & Deduct Inventory
    suspend fun deliverAndCloseOrder(orderId: Long, paymentMethod: String, amountPaid: Double) {
        val order = orderDao.getOrderByIdDirect(orderId) ?: return
        val parts = orderPartDao.getPartsForOrderDirect(orderId)

        // Deduct parts from inventory if not already closed
        if (!order.isClosed) {
            for (p in parts) {
                if (p.partId != null && p.partId > 0) {
                    inventoryDao.deductStock(p.partId, p.quantity)
                }
            }
        }

        val updatedOrder = order.copy(
            status = OrderStatus.ENTREGADO.name,
            deliveredAt = System.currentTimeMillis(),
            paymentMethod = paymentMethod,
            finalAmountPaid = amountPaid,
            isClosed = true
        )
        orderDao.updateOrder(updatedOrder)
    }

    // Create Warranty Order linked to an original order
    suspend fun createWarrantyOrder(
        parentOrder: RepairOrder,
        reportedIssue: String,
        warrantyCost: Double = 0.0,
        conditionNotes: String = ""
    ): Long {
        val count = orderDao.getOrdersCount() + 1
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val formattedNumber = "GAR-$year-${String.format(Locale.US, "%04d", count)}"

        val warrantyOrder = RepairOrder(
            orderNumber = formattedNumber,
            clientName = parentOrder.clientName,
            clientPhone = parentOrder.clientPhone,
            deviceBrand = parentOrder.deviceBrand,
            deviceModel = parentOrder.deviceModel,
            imeiOrSerial = parentOrder.imeiOrSerial,
            reportedIssue = reportedIssue,
            deviceConditionNotes = conditionNotes.ifBlank { "Reingreso por garantía de orden ${parentOrder.orderNumber}" },
            createdAt = System.currentTimeMillis(),
            status = OrderStatus.INGRESADO.name,
            isWarrantyOrder = true,
            parentWarrantyOrderId = parentOrder.id,
            warrantyCost = warrantyCost,
            budgetEstimated = warrantyCost,
            finalTotal = warrantyCost
        )
        return orderDao.insertOrder(warrantyOrder)
    }

    // Inventory
    val allInventory: Flow<List<InventoryPart>> = inventoryDao.getAllParts()
    suspend fun getAllInventoryDirect(): List<InventoryPart> = inventoryDao.getAllPartsDirect()
    suspend fun saveInventoryPart(part: InventoryPart): Long = inventoryDao.insertPart(part)
    suspend fun updateInventoryPart(part: InventoryPart) = inventoryDao.updatePart(part)
    suspend fun deleteInventoryPart(partId: Long) = inventoryDao.deletePartById(partId)
    suspend fun addStock(partId: Long, qty: Int) = inventoryDao.addStock(partId, qty)

    // Clear entire application for factory reset
    suspend fun wipeAllData() {
        orderPartDao.clearAll()
        orderDao.clearAllOrders()
        inventoryDao.clearAll()
        configDao.clearConfig()
    }

    // Direct access for Backup/Restore
    suspend fun getAllOrdersDirect(): List<RepairOrder> = orderDao.getAllOrdersDirect()
    suspend fun getAllOrderPartsDirect(): List<OrderItemPart> = orderPartDao.getAllOrderPartsDirect()
    suspend fun restoreDatabase(
        config: WorkshopConfig?,
        orders: List<RepairOrder>,
        parts: List<OrderItemPart>,
        inventory: List<InventoryPart>
    ) {
        orderPartDao.clearAll()
        orderDao.clearAllOrders()
        inventoryDao.clearAll()
        if (config != null) {
            configDao.insertOrUpdate(config)
        }
        for (inv in inventory) {
            inventoryDao.insertPart(inv)
        }
        for (ord in orders) {
            orderDao.insertOrder(ord)
        }
        for (p in parts) {
            orderPartDao.insertPart(p)
        }
    }
}
