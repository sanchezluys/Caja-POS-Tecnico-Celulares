package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OrderItemPart
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemPartDao {
    @Query("SELECT * FROM order_item_parts WHERE orderId = :orderId ORDER BY id ASC")
    fun getPartsForOrder(orderId: Long): Flow<List<OrderItemPart>>

    @Query("SELECT * FROM order_item_parts WHERE orderId = :orderId ORDER BY id ASC")
    suspend fun getPartsForOrderDirect(orderId: Long): List<OrderItemPart>

    @Query("SELECT * FROM order_item_parts")
    suspend fun getAllOrderPartsDirect(): List<OrderItemPart>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: OrderItemPart): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<OrderItemPart>)

    @Update
    suspend fun updatePart(part: OrderItemPart)

    @Delete
    suspend fun deletePart(part: OrderItemPart)

    @Query("DELETE FROM order_item_parts WHERE orderId = :orderId")
    suspend fun deletePartsForOrder(orderId: Long)

    @Query("DELETE FROM order_item_parts WHERE id = :id")
    suspend fun deletePartById(id: Long)

    @Query("DELETE FROM order_item_parts")
    suspend fun clearAll()
}
