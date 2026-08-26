package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.InventoryPart
import com.example.data.model.OrderItemPart
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvBackupHelper {

    private const val SECTION_CONFIG = "=== CONFIGURACION ==="
    private const val SECTION_ORDERS = "=== ORDENES_SERVICIO ==="
    private const val SECTION_ORDER_PARTS = "=== REPUESTOS_ORDENES ==="
    private const val SECTION_INVENTORY = "=== INVENTARIO_REPUESTOS ==="

    fun exportToCsvContent(
        config: WorkshopConfig?,
        orders: List<RepairOrder>,
        orderParts: List<OrderItemPart>,
        inventory: List<InventoryPart>
    ): String {
        return buildString {
            // Header
            append("# COPIA DE SEGURIDAD SERVICIO TECNICO CELULARES\n")
            append("# Fecha de exportacion: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n\n")

            // 1. Config
            append("$SECTION_CONFIG\n")
            append("id,workshopName,technicianName,phone,address,taxId,receiptFooter,showWarrantyTerms,currency,thousandSeparator,isConfigured\n")
            if (config != null) {
                append("${config.id},\"${escapeCsv(config.workshopName)}\",\"${escapeCsv(config.technicianName)}\",\"${escapeCsv(config.phone)}\",\"${escapeCsv(config.address)}\",\"${escapeCsv(config.taxId)}\",\"${escapeCsv(config.receiptFooter)}\",${config.showWarrantyTerms},\"${escapeCsv(config.currency)}\",\"${escapeCsv(config.thousandSeparator)}\",${config.isConfigured}\n")
            }
            append("\n")

            // 2. Inventory
            append("$SECTION_INVENTORY\n")
            append("id,code,name,brandCompatibility,purchaseCost,salePrice,stockQuantity,minStockAlert,createdAt\n")
            for (inv in inventory) {
                append("${inv.id},\"${escapeCsv(inv.code)}\",\"${escapeCsv(inv.name)}\",\"${escapeCsv(inv.brandCompatibility)}\",${inv.purchaseCost},${inv.salePrice},${inv.stockQuantity},${inv.minStockAlert},${inv.createdAt}\n")
            }
            append("\n")

            // 3. Orders
            append("$SECTION_ORDERS\n")
            append("id,orderNumber,clientName,clientPhone,deviceBrand,deviceModel,imeiOrSerial,imei2,reportedIssue,deviceConditionNotes,createdAt,deliveredAt,status,repairOutcome,repairDiagnosis,budgetEstimated,laborCost,partsTotalCost,partsTotalPrice,finalTotal,finalAmountPaid,paymentMethod,isWarrantyOrder,parentWarrantyOrderId,warrantyCost,isClosed\n")
            for (ord in orders) {
                append("${ord.id},\"${escapeCsv(ord.orderNumber)}\",\"${escapeCsv(ord.clientName)}\",\"${escapeCsv(ord.clientPhone)}\",\"${escapeCsv(ord.deviceBrand)}\",\"${escapeCsv(ord.deviceModel)}\",\"${escapeCsv(ord.imeiOrSerial)}\",\"${escapeCsv(ord.imei2)}\",\"${escapeCsv(ord.reportedIssue)}\",\"${escapeCsv(ord.deviceConditionNotes)}\",${ord.createdAt},${ord.deliveredAt ?: ""},\"${ord.status}\",\"${ord.repairOutcome ?: ""}\",\"${escapeCsv(ord.repairDiagnosis)}\",${ord.budgetEstimated},${ord.laborCost},${ord.partsTotalCost},${ord.partsTotalPrice},${ord.finalTotal},${ord.finalAmountPaid},\"${escapeCsv(ord.paymentMethod)}\",${ord.isWarrantyOrder},${ord.parentWarrantyOrderId ?: ""},${ord.warrantyCost},${ord.isClosed}\n")
            }
            append("\n")

            // 4. Order Item Parts
            append("$SECTION_ORDER_PARTS\n")
            append("id,orderId,partId,partName,quantity,unitCost,unitPrice\n")
            for (p in orderParts) {
                append("${p.id},${p.orderId},${p.partId ?: ""},\"${escapeCsv(p.partName)}\",${p.quantity},${p.unitCost},${p.unitPrice}\n")
            }
        }
    }

    data class ParsedBackup(
        val config: WorkshopConfig?,
        val orders: List<RepairOrder>,
        val orderParts: List<OrderItemPart>,
        val inventory: List<InventoryPart>
    )

    fun parseCsvContent(content: String): ParsedBackup {
        var currentSection = ""
        var config: WorkshopConfig? = null
        val orders = mutableListOf<RepairOrder>()
        val orderParts = mutableListOf<OrderItemPart>()
        val inventory = mutableListOf<InventoryPart>()

        val lines = content.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            if (line.startsWith("===")) {
                currentSection = line
                continue
            }

            // Skip table header lines
            if (line.startsWith("id,") || line.startsWith("id\t")) continue

            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue

            try {
                when (currentSection) {
                    SECTION_CONFIG -> {
                        if (tokens.size >= 8) {
                            val has11Cols = tokens.size >= 11
                            val hasSeparatorCol = tokens.size >= 10
                            val showTerms = if (has11Cols) tokens[7].toBooleanStrictOrNull() ?: true else true
                            val currVal = if (has11Cols) tokens.getOrElse(8) { "COP" } else tokens.getOrElse(7) { "COP" }
                            val sepVal = if (has11Cols) tokens.getOrElse(9) { "DOT" } else if (hasSeparatorCol) tokens.getOrElse(8) { "DOT" } else "DOT"
                            val isConfVal = if (has11Cols) tokens.getOrElse(10) { "true" } else if (hasSeparatorCol) tokens.getOrElse(9) { "true" } else tokens.getOrElse(8) { "true" }
                            config = WorkshopConfig(
                                id = tokens[0].toIntOrNull() ?: 1,
                                workshopName = tokens.getOrElse(1) { "" },
                                technicianName = tokens.getOrElse(2) { "" },
                                phone = tokens.getOrElse(3) { "" },
                                address = tokens.getOrElse(4) { "" },
                                taxId = tokens.getOrElse(5) { "" },
                                receiptFooter = tokens.getOrElse(6) { "" },
                                showWarrantyTerms = showTerms,
                                currency = if (currVal.isNotBlank()) currVal else "COP",
                                thousandSeparator = if (sepVal.isNotBlank()) sepVal else "DOT",
                                isConfigured = isConfVal.toBoolean()
                            )
                        }
                    }
                    SECTION_INVENTORY -> {
                        if (tokens.size >= 7) {
                            val inv = InventoryPart(
                                id = tokens[0].toLongOrNull() ?: 0L,
                                code = tokens.getOrElse(1) { "" },
                                name = tokens.getOrElse(2) { "" },
                                brandCompatibility = tokens.getOrElse(3) { "Universal" },
                                purchaseCost = tokens.getOrElse(4) { "0" }.toDoubleOrNull() ?: 0.0,
                                salePrice = tokens.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0,
                                stockQuantity = tokens.getOrElse(6) { "0" }.toIntOrNull() ?: 0,
                                minStockAlert = tokens.getOrElse(7) { "2" }.toIntOrNull() ?: 2,
                                createdAt = tokens.getOrElse(8) { "0" }.toLongOrNull() ?: System.currentTimeMillis()
                            )
                            inventory.add(inv)
                        }
                    }
                    SECTION_ORDERS -> {
                        if (tokens.size >= 15) {
                            val hasImei2Column = tokens.size >= 26
                            val imei1Val = tokens.getOrElse(6) { "" }
                            val imei2Val = if (hasImei2Column) tokens.getOrElse(7) { "" } else ""
                            val shift = if (hasImei2Column) 1 else 0

                            val ord = RepairOrder(
                                id = tokens[0].toLongOrNull() ?: 0L,
                                orderNumber = tokens.getOrElse(1) { "" },
                                clientName = tokens.getOrElse(2) { "" },
                                clientPhone = tokens.getOrElse(3) { "" },
                                deviceBrand = tokens.getOrElse(4) { "" },
                                deviceModel = tokens.getOrElse(5) { "" },
                                imeiOrSerial = imei1Val,
                                imei2 = imei2Val,
                                reportedIssue = tokens.getOrElse(7 + shift) { "" },
                                deviceConditionNotes = tokens.getOrElse(8 + shift) { "" },
                                createdAt = tokens.getOrElse(9 + shift) { "0" }.toLongOrNull() ?: System.currentTimeMillis(),
                                deliveredAt = tokens.getOrElse(10 + shift) { "" }.toLongOrNull(),
                                status = tokens.getOrElse(11 + shift) { "INGRESADO" },
                                repairOutcome = tokens.getOrElse(12 + shift) { "" }.ifBlank { null },
                                repairDiagnosis = tokens.getOrElse(13 + shift) { "" },
                                budgetEstimated = tokens.getOrElse(14 + shift) { "0" }.toDoubleOrNull() ?: 0.0,
                                laborCost = tokens.getOrElse(15 + shift) { "0" }.toDoubleOrNull() ?: 0.0,
                                partsTotalCost = tokens.getOrElse(16 + shift) { "0" }.toDoubleOrNull() ?: 0.0,
                                partsTotalPrice = tokens.getOrElse(17 + shift) { "0" }.toDoubleOrNull() ?: 0.0,
                                finalTotal = tokens.getOrElse(18 + shift) { "0" }.toDoubleOrNull() ?: 0.0,
                                finalAmountPaid = tokens.getOrElse(19 + shift) { "0" }.toDoubleOrNull() ?: 0.0,
                                paymentMethod = tokens.getOrElse(20 + shift) { "Efectivo" },
                                isWarrantyOrder = tokens.getOrElse(21 + shift) { "false" }.toBoolean(),
                                parentWarrantyOrderId = tokens.getOrElse(22 + shift) { "" }.toLongOrNull(),
                                warrantyCost = tokens.getOrElse(23 + shift) { "0" }.toDoubleOrNull() ?: 0.0,
                                isClosed = tokens.getOrElse(24 + shift) { "false" }.toBoolean()
                            )
                            orders.add(ord)
                        }
                    }
                    SECTION_ORDER_PARTS -> {
                        if (tokens.size >= 6) {
                            val part = OrderItemPart(
                                id = tokens[0].toLongOrNull() ?: 0L,
                                orderId = tokens.getOrElse(1) { "0" }.toLongOrNull() ?: 0L,
                                partId = tokens.getOrElse(2) { "" }.toLongOrNull(),
                                partName = tokens.getOrElse(3) { "" },
                                quantity = tokens.getOrElse(4) { "1" }.toIntOrNull() ?: 1,
                                unitCost = tokens.getOrElse(5) { "0" }.toDoubleOrNull() ?: 0.0,
                                unitPrice = tokens.getOrElse(6) { "0" }.toDoubleOrNull() ?: 0.0
                            )
                            orderParts.add(part)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return ParsedBackup(config, orders, orderParts, inventory)
    }

    fun readTextFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        return BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
    }

    fun createBackupFileUri(context: Context, csvContent: String): Uri? {
        return try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(context.cacheDir, "backup_serviciotecnico_$dateStr.csv")
            file.writeText(csvContent)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    curVal.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
            i++
        }
        result.add(curVal.toString().trim())
        return result
    }
}
