package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WorkshopConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkshopConfigDao {
    @Query("SELECT * FROM workshop_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<WorkshopConfig?>

    @Query("SELECT * FROM workshop_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): WorkshopConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: WorkshopConfig)

    @Query("DELETE FROM workshop_config")
    suspend fun clearConfig()
}
