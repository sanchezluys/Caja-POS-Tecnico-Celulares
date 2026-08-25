package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.OrderItemPart
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptGenerator {

    fun generateReceiptPdf(
        context: Context,
        order: RepairOrder,
        parts: List<OrderItemPart>,
        config: WorkshopConfig
    ): Uri? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width in points (72 dpi)
            val pageHeight = 842 // Standard A4 height in points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(order.createdAt))
            val currency = config.currency.ifBlank { "$" }

            // 1. Header background banner
            paint.color = Color.parseColor("#0F172A") // Deep Slate/Navy
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, paint)

            // Workshop Name
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            val workshopTitle = config.workshopName.ifBlank { "SERVICIO TÉCNICO DE CELULARES" }
            canvas.drawText(workshopTitle.uppercase(Locale.getDefault()), 28f, 38f, paint)

            // Workshop details
            paint.textSize = 10f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#94A3B8") // Light slate
            var headerY = 56f
            if (config.technicianName.isNotBlank()) {
                canvas.drawText("Técnico Especialista: ${config.technicianName}", 28f, headerY, paint)
                headerY += 15f
            }
            val contactInfo = listOfNotNull(
                if (config.phone.isNotBlank()) "Tel/WhatsApp: ${config.phone}" else null,
                if (config.taxId.isNotBlank()) "RIF/ID: ${config.taxId}" else null
            ).joinToString(" | ")
            if (contactInfo.isNotBlank()) {
                canvas.drawText(contactInfo, 28f, headerY, paint)
                headerY += 15f
            }
            if (config.address.isNotBlank()) {
                canvas.drawText("Dirección: ${config.address}", 28f, headerY, paint)
            }

            // Order Number Badge in header right
            paint.color = Color.parseColor("#0284C7") // Electric Blue
            val badgeRect = RectF(pageWidth - 210f, 25f, pageWidth - 28f, 75f)
            canvas.drawRoundRect(badgeRect, 8f, 8f, paint)
            paint.color = Color.WHITE
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("ORDEN DE SERVICIO", pageWidth - 195f, 44f, paint)
            paint.textSize = 14f
            canvas.drawText(order.orderNumber, pageWidth - 195f, 64f, paint)

            var currentY = 140f

            // Warranty Notice Banner if warranty order
            if (order.isWarrantyOrder) {
                paint.color = Color.parseColor("#FEF3C7") // Warm Amber
                val warnRect = RectF(28f, currentY, pageWidth - 28f, currentY + 28f)
                canvas.drawRoundRect(warnRect, 6f, 6f, paint)
                paint.color = Color.parseColor("#92400E")
                paint.textSize = 10f
                paint.isFakeBoldText = true
                canvas.drawText("★ ORDEN DE GARANTÍA (Costo de Garantía: $currency ${String.format(Locale.US, "%.2f", order.warrantyCost)})", 40f, currentY + 18f, paint)
                currentY += 38f
            }

            // 2. Client and Device Section (Two columns)
            // Left Column: Client Info
            paint.color = Color.parseColor("#F1F5F9") // Light gray container
            val clientRect = RectF(28f, currentY, (pageWidth / 2f) - 8f, currentY + 95f)
            canvas.drawRoundRect(clientRect, 6f, 6f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("DATOS DEL CLIENTE", 38f, currentY + 20f, paint)

            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#334155")
            canvas.drawText("Nombre: ${order.clientName}", 38f, currentY + 40f, paint)
            canvas.drawText("Teléfono / WhatsApp: ${order.clientPhone.ifBlank { "No registrado" }}", 38f, currentY + 58f, paint)
            canvas.drawText("Fecha de Ingreso: $dateStr", 38f, currentY + 76f, paint)

            // Right Column: Device Info
            val deviceRect = RectF((pageWidth / 2f) + 8f, currentY, pageWidth - 28f, currentY + 95f)
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(deviceRect, 6f, 6f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("DATOS DEL DISPOSITIVO", (pageWidth / 2f) + 18f, currentY + 20f, paint)

            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#334155")
            canvas.drawText("Equipo: ${order.deviceBrand} ${order.deviceModel}", (pageWidth / 2f) + 18f, currentY + 38f, paint)
            val imeiText = if (order.imei2.isNotBlank()) {
                "IMEI 1: ${order.imeiOrSerial.ifBlank { "N/A" }} | IMEI 2: ${order.imei2}"
            } else {
                "IMEI / Serial: ${order.imeiOrSerial.ifBlank { "N/A" }}"
            }
            canvas.drawText(imeiText.take(48), (pageWidth / 2f) + 18f, currentY + 54f, paint)
            val estadoText = when (order.status) {
                "LISTO_ENTREGA" -> "Listo para entrega"
                "ENTREGADO" -> "Entregado y Cobrado"
                "NO_REPARADO" -> "No Reparado"
                "CANCELADO" -> "Cancelado"
                else -> order.status
            }
            canvas.drawText("Estado: $estadoText", (pageWidth / 2f) + 18f, currentY + 72f, paint)

            currentY += 105f

            // 3. Issue Reported & Technical Diagnosis Box
            paint.color = Color.parseColor("#F8FAFC")
            val issueRect = RectF(28f, currentY, pageWidth - 28f, currentY + 80f)
            canvas.drawRoundRect(issueRect, 6f, 6f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 10.5f
            paint.isFakeBoldText = true
            canvas.drawText("PROBLEMA REPORTADO POR EL CLIENTE:", 38f, currentY + 20f, paint)
            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#475569")
            canvas.drawText(order.reportedIssue.take(80), 38f, currentY + 36f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 10.5f
            paint.isFakeBoldText = true
            canvas.drawText("DIAGNÓSTICO TÉCNICO / TRABAJO REALIZADO:", 38f, currentY + 56f, paint)
            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#475569")
            val diagText = order.repairDiagnosis.ifBlank {
                if (order.repairOutcome == "EXITOSA") "Reparación completada satisfactoriamente con pruebas operativas."
                else "Diagnóstico en proceso o no completado."
            }
            canvas.drawText(diagText.take(80), 38f, currentY + 72f, paint)

            currentY += 92f

            // 4. Parts / Spare parts Table
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("DETALLE DE REPUESTOS Y MANO DE OBRA", 28f, currentY + 14f, paint)
            currentY += 22f

            // Table Header
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRect(28f, currentY, pageWidth - 28f, currentY + 22f, paint)
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            canvas.drawText("Cant.", 36f, currentY + 15f, paint)
            canvas.drawText("Descripción / Repuesto", 75f, currentY + 15f, paint)
            canvas.drawText("P. Unitario", pageWidth - 145f, currentY + 15f, paint)
            canvas.drawText("Total", pageWidth - 70f, currentY + 15f, paint)
            currentY += 22f

            paint.isFakeBoldText = false
            paint.textSize = 9f

            if (parts.isEmpty()) {
                paint.color = Color.parseColor("#64748B")
                canvas.drawText("No se cargaron repuestos adicionales para esta orden.", 36f, currentY + 16f, paint)
                currentY += 22f
            } else {
                for (p in parts) {
                    paint.color = Color.parseColor("#334155")
                    canvas.drawText("${p.quantity}", 36f, currentY + 16f, paint)
                    canvas.drawText(p.partName.take(45), 75f, currentY + 16f, paint)
                    canvas.drawText("$currency ${String.format(Locale.US, "%.2f", p.unitPrice)}", pageWidth - 145f, currentY + 16f, paint)
                    canvas.drawText("$currency ${String.format(Locale.US, "%.2f", p.unitPrice * p.quantity)}", pageWidth - 70f, currentY + 16f, paint)

                    // Light line separator
                    paint.color = Color.parseColor("#F1F5F9")
                    canvas.drawLine(28f, currentY + 20f, pageWidth - 28f, currentY + 20f, paint)
                    currentY += 20f
                }
            }

            currentY += 10f

            // 5. Totals Box (Right aligned)
            val totalsBoxLeft = pageWidth - 250f
            paint.color = Color.parseColor("#F8FAFC")
            val totalRect = RectF(totalsBoxLeft, currentY, pageWidth - 28f, currentY + 95f)
            canvas.drawRoundRect(totalRect, 6f, 6f, paint)

            paint.textSize = 9.5f
            paint.color = Color.parseColor("#475569")
            paint.isFakeBoldText = false
            canvas.drawText("Presupuesto Estimado:", totalsBoxLeft + 12f, currentY + 20f, paint)
            canvas.drawText("$currency ${String.format(Locale.US, "%.2f", order.budgetEstimated)}", pageWidth - 80f, currentY + 20f, paint)

            canvas.drawText("Mano de Obra:", totalsBoxLeft + 12f, currentY + 38f, paint)
            canvas.drawText("$currency ${String.format(Locale.US, "%.2f", order.laborCost)}", pageWidth - 80f, currentY + 38f, paint)

            canvas.drawText("Subtotal Repuestos:", totalsBoxLeft + 12f, currentY + 54f, paint)
            canvas.drawText("$currency ${String.format(Locale.US, "%.2f", order.partsTotalPrice)}", pageWidth - 80f, currentY + 54f, paint)

            // Highlighted final total
            paint.color = Color.parseColor("#0284C7")
            val finalTotalBar = RectF(totalsBoxLeft, currentY + 66f, pageWidth - 28f, currentY + 95f)
            canvas.drawRoundRect(finalTotalBar, 4f, 4f, paint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("TOTAL A PAGAR:", totalsBoxLeft + 12f, currentY + 86f, paint)
            canvas.drawText("$currency ${String.format(Locale.US, "%.2f", order.finalTotal)}", pageWidth - 85f, currentY + 86f, paint)

            currentY += 115f

            // 6. Warranty Terms / Conditions
            paint.color = Color.parseColor("#F1F5F9")
            val policyRect = RectF(28f, currentY, pageWidth - 28f, currentY + 50f)
            canvas.drawRoundRect(policyRect, 6f, 6f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText("TÉRMINOS Y CONDICIONES DE GARANTÍA:", 38f, currentY + 16f, paint)

            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#475569")
            paint.textSize = 8.5f
            val policyText = config.receiptFooter.ifBlank {
                "Garantía de 30 días en servicio técnico y repuestos instalados por defectos de fábrica. No cubre humedad ni golpes posteriores."
            }
            canvas.drawText(policyText.take(110), 38f, currentY + 32f, paint)

            currentY += 80f

            // 7. Signature placeholders
            paint.color = Color.parseColor("#94A3B8")
            paint.strokeWidth = 1f
            // Left signature: Technician
            canvas.drawLine(40f, currentY, (pageWidth / 2f) - 40f, currentY, paint)
            paint.textSize = 9f
            paint.isFakeBoldText = false
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Firma del Técnico Responsable", 60f, currentY + 15f, paint)

            // Right signature: Customer
            canvas.drawLine((pageWidth / 2f) + 40f, currentY, pageWidth - 40f, currentY, paint)
            canvas.drawText("Firma y Conformidad del Cliente", (pageWidth / 2f) + 60f, currentY + 15f, paint)

            pdfDocument.finishPage(page)

            // Save PDF to cache dir
            val cleanOrderNumber = order.orderNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val file = File(context.cacheDir, "recibo_$cleanOrderNumber.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

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
}
