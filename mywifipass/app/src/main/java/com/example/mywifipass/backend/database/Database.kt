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

@Serializable
@Entity(tableName = "networks")
data class Network(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val id: Int = 0,
    val user_name: String,
    val user_email: String,
    val user_id_document: String,
    var ca_certificate: String,
    var certificate: String,
    var private_key: String,
    val ssid: String,
    val network_common_name: String,
    val user_uuid: String,
    val location: String,
    val start_date: String,
    val end_date: String,
    val description: String,
    val location_name: String,
    val validation_url: String,
    val certificates_symmetric_key_url: String,
    val location_uuid: String,
    var has_downloaded_url: String = "",

    @Transient
    var certificates_symmetric_key: String = "",
    @Transient
    var is_connection_configured: Boolean = false,
    @Transient
    var is_certificates_key_set: Boolean = false,
    @Transient
    var are_certificiates_decrypted: Boolean = false,
)

@Database(entities = [Network::class], version = 5)
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

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE networks ADD COLUMN certificates_symmetric_key TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE networks ADD COLUMN is_certificates_key_set INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object: Migration (2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE networks ADD COLUMN validation_url TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE networks ADD COLUMN certificates_symmetric_key_url TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_3_4 = object: Migration (3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE networks ADD COLUMN location_uuid TEXT NOT NULL DEFAULT ''")
    }
}
private val MIGRATION_4_5 = object: Migration (4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE networks ADD COLUMN has_downloaded_url TEXT NOT NULL DEFAULT ''")
    }
}
class DataSource(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "app-database"
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()

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