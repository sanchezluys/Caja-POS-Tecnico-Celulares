package com.example

import com.example.data.model.InventoryPart
import com.example.data.model.OrderItemPart
import com.example.data.model.OrderStatus
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig
import com.example.util.CsvBackupHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkshopLogicTest {

    @Test
    fun testOrderStatusLabelsAndCodes() {
        assertEquals("Ingresado", OrderStatus.INGRESADO.label)
        assertEquals("En Diagnóstico", OrderStatus.EN_DIAGNOSTICO.label)
        assertEquals("En Reparación", OrderStatus.EN_REPARACION.label)
        assertEquals("Esperando Repuesto", OrderStatus.ESPERANDO_REPUESTO.label)
        assertEquals("Listo para Entrega", OrderStatus.LISTO_ENTREGA.label)
        assertEquals("Entregado", OrderStatus.ENTREGADO.label)
        assertEquals("No Reparado", OrderStatus.NO_REPARADO.label)
        assertEquals("Cancelado", OrderStatus.CANCELADO.label)
    }

    @Test
    fun testFinancialCalculationsForRepairOrder() {
        val parts = listOf(
            OrderItemPart(orderId = 1, partName = "Pantalla OLED", quantity = 1, unitCost = 25.0, unitPrice = 50.0),
            OrderItemPart(orderId = 1, partName = "Batería", quantity = 1, unitCost = 10.0, unitPrice = 20.0)
        )
        val laborCost = 15.0

        val partsWorkshopCost = parts.sumOf { it.unitCost * it.quantity }
        val partsTotalPrice = parts.sumOf { it.unitPrice * it.quantity }
        val finalTotal = laborCost + partsTotalPrice
        val netProfit = finalTotal - partsWorkshopCost

        assertEquals(35.0, partsWorkshopCost, 0.001)
        assertEquals(70.0, partsTotalPrice, 0.001)
        assertEquals(85.0, finalTotal, 0.001)
        assertEquals(50.0, netProfit, 0.001) // 85 total charged - 35 workshop cost = 50 net profit
    }

    @Test
    fun testCsvExportAndImportParsing() {
        val config = WorkshopConfig(
            id = 1,
            workshopName = "TechFix Master",
            technicianName = "Luis Sánchez",
            phone = "+584121234567",
            currency = "$",
            isConfigured = true
        )

        val orders = listOf(
            RepairOrder(
                id = 1,
                orderNumber = "ORD-2026-0001",
                clientName = "Pedro Pérez",
                clientPhone = "+584149876543",
                deviceBrand = "Samsung",
                deviceModel = "Galaxy A54",
                reportedIssue = "Pantalla rota",
                status = OrderStatus.ENTREGADO.name,
                laborCost = 20.0,
                partsTotalPrice = 45.0,
                partsTotalCost = 20.0,
                finalTotal = 65.0,
                finalAmountPaid = 65.0,
                isClosed = true
            )
        )

        val parts = listOf(
            OrderItemPart(
                id = 1,
                orderId = 1,
                partName = "Pantalla Samsung A54",
                quantity = 1,
                unitCost = 20.0,
                unitPrice = 45.0
            )
        )

        val inventory = listOf(
            InventoryPart(
                id = 1,
                code = "DISP-SAM-A54",
                name = "Pantalla Samsung A54",
                brandCompatibility = "Samsung",
                purchaseCost = 20.0,
                salePrice = 45.0,
                stockQuantity = 5,
                minStockAlert = 2
            )
        )

        val csvString = CsvBackupHelper.exportToCsvContent(config, orders, parts, inventory)
        assertTrue(csvString.contains("[CONFIG]"))
        assertTrue(csvString.contains("[ORDERS]"))
        assertTrue(csvString.contains("[ORDER_PARTS]"))
        assertTrue(csvString.contains("[INVENTORY]"))

        val parsed = CsvBackupHelper.parseCsvContent(csvString)
        assertNotNull(parsed.config)
        assertEquals("TechFix Master", parsed.config?.workshopName)
        assertEquals(1, parsed.orders.size)
        assertEquals("ORD-2026-0001", parsed.orders[0].orderNumber)
        assertEquals("Pedro Pérez", parsed.orders[0].clientName)
        assertEquals(1, parsed.orderParts.size)
        assertEquals(1, parsed.inventory.size)
        assertEquals(5, parsed.inventory[0].stockQuantity)
    }

    @Test
    fun testWarrantyOrderAttributes() {
        val parentOrder = RepairOrder(
            id = 1,
            orderNumber = "ORD-2026-0001",
            clientName = "Maria Gomez",
            clientPhone = "+584241112233",
            deviceBrand = "Xiaomi",
            deviceModel = "Redmi Note 12",
            reportedIssue = "No carga"
        )

        val warrantyOrder = RepairOrder(
            id = 2,
            orderNumber = "GAR-2026-0002",
            isWarrantyOrder = true,
            parentOrderId = parentOrder.id,
            clientName = parentOrder.clientName,
            clientPhone = parentOrder.clientPhone,
            deviceBrand = parentOrder.deviceBrand,
            deviceModel = parentOrder.deviceModel,
            reportedIssue = "Falla de carga en garantía",
            warrantyCost = 0.0,
            finalTotal = 0.0
        )

        assertTrue(warrantyOrder.isWarrantyOrder)
        assertEquals(parentOrder.id, warrantyOrder.parentOrderId)
        assertEquals(0.0, warrantyOrder.warrantyCost, 0.001)
        assertEquals("Maria Gomez", warrantyOrder.clientName)
    }
}
