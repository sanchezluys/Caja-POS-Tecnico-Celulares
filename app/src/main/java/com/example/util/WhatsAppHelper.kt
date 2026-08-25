package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig
import java.net.URLEncoder
import java.util.Locale

object WhatsAppHelper {

    fun formatPhoneNumberForWhatsApp(phone: String): String {
        // Remove spaces, hyphens, parentheses, plus
        return phone.replace("[^0-9]".toRegex(), "")
    }

    fun buildNotificationMessage(
        order: RepairOrder,
        config: WorkshopConfig,
        customNote: String = ""
    ): String {
        val currency = config.currency.ifBlank { "$" }
        val workshop = config.workshopName.ifBlank { "Servicio Técnico" }
        val statusText = when (order.status) {
            "LISTO_ENTREGA" -> "¡Su equipo está LISTO para ser retirado!"
            "ENTREGADO" -> "Equipo entregado satisfactoriamente."
            "EN_REPARACION" -> "Su equipo se encuentra actualmente EN REPARACIÓN."
            "ESPERANDO_REPUESTO" -> "Estamos a la espera del repuesto correspondiente."
            "NO_REPARADO" -> "Diagnóstico finalizado (Equipo no reparado)."
            else -> "Actualización de su orden de servicio."
        }

        val outcomeText = if (order.repairOutcome == "EXITOSA") "✓ Reparación Exitosa" else if (order.repairOutcome != null) "✗ ${order.repairOutcome}" else ""

        return buildString {
            append("🔧 *${workshop.uppercase(Locale.getDefault())}*\n")
            if (config.technicianName.isNotBlank()) {
                append("👤 Técnico: ${config.technicianName}\n")
            }
            append("━━━━━━━━━━━━━━━━━━\n")
            append("📄 *Orden N°:* ${order.orderNumber}\n")
            append("👋 Hola *${order.clientName}*,\n")
            append("📱 *Equipo:* ${order.deviceBrand} ${order.deviceModel}\n")
            append("📌 *Estado:* $statusText\n")
            if (outcomeText.isNotBlank()) {
                append("⚙️ *Resultado:* $outcomeText\n")
            }
            if (order.isWarrantyOrder) {
                append("★ *Servicio de Garantía*\n")
            }
            append("💰 *Total:* $currency ${String.format(Locale.US, "%.2f", order.finalTotal)}\n")
            if (order.isClosed && order.finalAmountPaid > 0) {
                append("💳 *Pagado:* $currency ${String.format(Locale.US, "%.2f", order.finalAmountPaid)} (${order.paymentMethod})\n")
            }
            if (customNote.isNotBlank()) {
                append("📝 *Nota:* $customNote\n")
            }
            if (config.receiptFooter.isNotBlank()) {
                append("\nℹ️ *Garantía:* ${config.receiptFooter}\n")
            }
            if (config.address.isNotBlank()) {
                append("📍 *Dirección:* ${config.address}\n")
            }
            append("\n¡Gracias por confiar en nosotros!")
        }
    }

    fun sharePdfReceipt(
        context: Context,
        pdfUri: Uri,
        order: RepairOrder,
        config: WorkshopConfig
    ) {
        val message = buildNotificationMessage(order, config)
        val cleanPhone = formatPhoneNumberForWhatsApp(order.clientPhone)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "Recibo de Servicio Técnico ${order.orderNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (cleanPhone.isNotBlank()) {
                // Target WhatsApp if available
                setPackage("com.whatsapp")
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to chooser without package lock
            val chooserIntent = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pdfUri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Enviar Recibo PDF vía..."
            )
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }

    fun openWhatsAppChat(
        context: Context,
        phone: String,
        message: String
    ) {
        val cleanPhone = formatPhoneNumberForWhatsApp(phone)
        val encodedMessage = URLEncoder.encode(message, "UTF-8")
        val url = if (cleanPhone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If browser or whatsapp not directly opened, generic view
            val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(Intent.createChooser(genericIntent, "Abrir WhatsApp"))
        }
    }
}
