package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.InventoryPartDao
import com.example.data.dao.OrderItemPartDao
import com.example.data.dao.RepairOrderDao
import com.example.data.dao.WorkshopConfigDao
import com.example.data.model.InventoryPart
import com.example.data.model.OrderItemPart
import com.example.data.model.RepairOrder
import com.example.data.model.WorkshopConfig

@Database(
    entities = [
        WorkshopConfig::class,
        RepairOrder::class,
        OrderItemPart::class,
        InventoryPart::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workshopConfigDao(): WorkshopConfigDao
    abstract fun repairOrderDao(): RepairOrderDao
    abstract fun orderItemPartDao(): OrderItemPartDao
    abstract fun inventoryPartDao(): InventoryPartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tech_repair_service.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
