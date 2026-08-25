package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InventoryPart
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryPartDao {
    @Query("SELECT * FROM inventory_parts ORDER BY name ASC")
    fun getAllParts(): Flow<List<InventoryPart>>

    @Query("SELECT * FROM inventory_parts WHERE id = :id LIMIT 1")
    suspend fun getPartByIdDirect(id: Long): InventoryPart?

    @Query("SELECT * FROM inventory_parts ORDER BY name ASC")
    suspend fun getAllPartsDirect(): List<InventoryPart>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: InventoryPart): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<InventoryPart>)

    @Update
    suspend fun updatePart(part: InventoryPart)

    @Query("UPDATE inventory_parts SET stockQuantity = CASE WHEN stockQuantity >= :qty THEN stockQuantity - :qty ELSE 0 END WHERE id = :partId")
    suspend fun deductStock(partId: Long, qty: Int)

    @Query("UPDATE inventory_parts SET stockQuantity = stockQuantity + :qty WHERE id = :partId")
    suspend fun addStock(partId: Long, qty: Int)

    @Delete
    suspend fun deletePart(part: InventoryPart)

    @Query("DELETE FROM inventory_parts WHERE id = :id")
    suspend fun deletePartById(id: Long)

    @Query("DELETE FROM inventory_parts")
    suspend fun clearAll()
}
