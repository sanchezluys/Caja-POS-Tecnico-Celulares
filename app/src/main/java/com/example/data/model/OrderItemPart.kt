package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_item_parts")
data class OrderItemPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val partId: Long? = null, // linked to InventoryPart if chosen from inventory
    val partName: String,
    val quantity: Int = 1,
    val unitCost: Double = 0.0,  // Workshop acquisition cost
    val unitPrice: Double = 0.0  // Sale price to client
)
