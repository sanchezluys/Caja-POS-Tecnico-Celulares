package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workshop_config")
data class WorkshopConfig(
    @PrimaryKey val id: Int = 1,
    val workshopName: String = "",
    val technicianName: String = "",
    val phone: String = "",
    val address: String = "",
    val taxId: String = "", // RIF / RUT / DNI / NIF
    val receiptFooter: String = "Garantía de 30 días en servicio técnico y repuestos instalados por defectos de fábrica. No cubre humedad ni golpes posteriores.",
    val showWarrantyTerms: Boolean = true,
    val currency: String = "COP",
    val thousandSeparator: String = "DOT", // "DOT", "COMMA", "NONE"
    val isConfigured: Boolean = false,
    val configuredAt: Long = System.currentTimeMillis()
)
