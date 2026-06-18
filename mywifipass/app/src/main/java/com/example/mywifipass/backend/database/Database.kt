/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

package app.mywifipass.backend.database

import kotlinx.serialization.*
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.util.Log
import java.util.concurrent.Executors
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.mywifipass.model.data.Network

// Migration from version 1 to version 2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE networks ADD COLUMN user_email TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE networks ADD COLUMN check_user_authorized_url TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE networks ADD COLUMN is_user_authorized INTEGER NOT NULL DEFAULT 0")
    }
}

// Migration from version 2 to version 3
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE networks ADD COLUMN requires_fido2_validation INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE networks ADD COLUMN fido2_authenticate_start_url TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE networks ADD COLUMN fido2_authenticate_finish_url TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE networks ADD COLUMN fido2_rp_id TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Network::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao
}


@Dao
interface NetworkDao {
    @Query("SELECT * FROM networks")
    fun getAllNetworks(): List<Network>
    
    @Insert
    fun insertNetwork(network: Network)

    @Insert
    fun insertNetworks(networkList: List<Network>)

    @Delete 
    fun deleteNetwork(network: Network)

    @Update
    fun updateNetwork(network: Network)
}

class DataSource(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "app-database"
    )
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    .build() 

    private val NetworkDao = db.networkDao()

    fun loadConnections(): List<Network> {
        return try {
            val Networks = NetworkDao.getAllNetworks()
            Networks
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun insertNetworks(Networks: List<Network>) {
        NetworkDao.insertNetworks(Networks)
    }

    fun insertNetwork(Network: Network) {
        NetworkDao.insertNetwork(Network)
    }

    fun deleteNetwork(Network: Network) {
        NetworkDao.deleteNetwork(Network)
    }

    fun updateNetwork(Network: Network) {
        NetworkDao.updateNetwork(Network)   
    }
}