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
    
    // @SerialName("name")
    // val user_name: String,
    
    // @SerialName("email")
    // val user_email: String,
    
    // @SerialName("id_document")
    // val user_id_document: String,
    
    // val user_uuid: String, //useless
    val network_common_name: String, 
    val ssid: String,
    val location: String="",
    val start_date: String,
    val end_date: String,
    val description: String="",
    val location_name: String,
    // val location_uuid: String, // useless
    val validation_url: String,
    val certificates_url: String,
    var has_downloaded_url: String,
    val certificates_symmetric_key: String,
    
    @Transient
    var ca_certificate: String = "",
    @Transient
    var certificate: String = "",
    @Transient
    var private_key: String = "",
    @Transient
    var is_connection_configured: Boolean = false,
    @Transient
    var is_certificates_key_set: Boolean = false,
    @Transient
    var are_certificiates_decrypted: Boolean = false,
)

@Database(entities = [Network::class], version = 1)
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
    ).build() 

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