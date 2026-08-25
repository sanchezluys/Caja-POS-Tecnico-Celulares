package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RepairOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairOrderDao {
    @Query("SELECT * FROM repair_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<RepairOrder>>

    @Query("SELECT * FROM repair_orders WHERE id = :id LIMIT 1")
    fun getOrderById(id: Long): Flow<RepairOrder?>

    @Query("SELECT * FROM repair_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderByIdDirect(id: Long): RepairOrder?

    @Query("SELECT * FROM repair_orders WHERE parentWarrantyOrderId = :parentId ORDER BY createdAt DESC")
    fun getWarrantyOrdersForParent(parentId: Long): Flow<List<RepairOrder>>

    @Query("SELECT * FROM repair_orders WHERE createdAt >= :startTimestamp AND createdAt <= :endTimestamp ORDER BY createdAt DESC")
    fun getOrdersInDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<RepairOrder>>

    @Query("SELECT * FROM repair_orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersDirect(): List<RepairOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: RepairOrder): Long

    @Update
    suspend fun updateOrder(order: RepairOrder)

    @Delete
    suspend fun deleteOrder(order: RepairOrder)

    @Query("DELETE FROM repair_orders WHERE id = :id")
    suspend fun deleteOrderById(id: Long)

    @Query("DELETE FROM repair_orders")
    suspend fun clearAllOrders()

    @Query("SELECT COUNT(*) FROM repair_orders")
    suspend fun getOrdersCount(): Int
}
