package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_parts")
data class InventoryPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String = "",
    val name: String,
    val brandCompatibility: String = "Universal",
    val purchaseCost: Double = 0.0,
    val salePrice: Double = 0.0,
    val stockQuantity: Int = 0,
    val minStockAlert: Int = 2,
    val createdAt: Long = System.currentTimeMillis()
)
