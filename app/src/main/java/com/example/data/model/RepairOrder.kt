package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OrderStatus(val label: String, val colorHex: Long) {
    INGRESADO("Ingresado", 0xFF0284C7),
    EN_DIAGNOSTICO("En Diagnóstico", 0xFF8B5CF6),
    ESPERANDO_REPUESTO("Esperando Repuesto", 0xFFF59E0B),
    EN_REPARACION("En Reparación", 0xFF3B82F6),
    LISTO_ENTREGA("Listo para Entrega", 0xFF10B981),
    ENTREGADO("Entregado / Cerrado", 0xFF059669),
    NO_REPARADO("No Reparado / Irreparable", 0xFFEF4444),
    CANCELADO("Cancelado", 0xFF6B7280)
}

enum class RepairOutcome(val label: String) {
    EXITOSA("Reparación Exitosa"),
    NO_EXITOSA("No Reparado / Sin Solución"),
    CANCELADA("Cancelada por Cliente")
}

@Entity(tableName = "repair_orders")
data class RepairOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val deviceBrand: String = "",
    val deviceModel: String = "",
    val imeiOrSerial: String = "",
    val imei2: String = "",
    val reportedIssue: String = "",
    val deviceConditionNotes: String = "",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val status: String = OrderStatus.INGRESADO.name,
    val repairOutcome: String? = null, // EXITOSA, NO_EXITOSA, CANCELADA
    val repairDiagnosis: String = "",
    val budgetEstimated: Double = 0.0,
    val laborCost: Double = 0.0,
    val partsTotalCost: Double = 0.0,
    val partsTotalPrice: Double = 0.0,
    val finalTotal: Double = 0.0,
    val finalAmountPaid: Double = 0.0,
    val paymentMethod: String = "Efectivo",
    val isWarrantyOrder: Boolean = false,
    val parentWarrantyOrderId: Long? = null,
    val warrantyCost: Double = 0.0,
    val isClosed: Boolean = false,
    val finalNotificationSent: Boolean = false
) {
    fun getPhotoList(): List<String> {
        if (photoUri.isNullOrBlank()) return emptyList()
        return photoUri.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        const val PHOTO_DELIMITER = "|||"

        fun joinPhotos(photos: List<String>): String? {
            val clean = photos.map { it.trim() }.filter { it.isNotEmpty() }
            return if (clean.isEmpty()) null else clean.joinToString(PHOTO_DELIMITER)
        }
    }
}
