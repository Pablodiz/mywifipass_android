package com.example.get_eap_tls.backend.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.util.Log
import java.util.concurrent.Executors

import com.example.get_eap_tls.backend.api_petitions.WifiNetworkLocation
import com.example.get_eap_tls.backend.certificates.EapTLSCertificate
import com.example.get_eap_tls.backend.api_petitions.ParsedReply

@Entity(tableName = "parsed_reply")
data class DatabaseParsedReply(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user_name: String,
    val user_email: String,
    val user_id_document: String,
    val ca_certificate: String,
    val certificate: String,
    val private_key: String,
    val ssid: String,
    val network_common_name: String
)

@Database(entities = [DatabaseParsedReply::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parsedReplyDao(): ParsedReplyDao
}


@Dao
interface ParsedReplyDao {
    @Query("SELECT * FROM parsed_reply")
    fun getAllParsedReplies(): List<DatabaseParsedReply>
    
    @Insert
    fun insertParsedReply(parsedReply: DatabaseParsedReply)

    @Insert
    fun insertParsedReplies(parsedReplies: List<DatabaseParsedReply>)

    @Delete 
    fun deleteParsedReply(parsedReply: DatabaseParsedReply)
}



class DataSource(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "app-database"
    ).build()

    private val parsedReplyDao = db.parsedReplyDao()

    fun loadConnections(): List<DatabaseParsedReply> {
        return try {
            val parsedReplies = parsedReplyDao.getAllParsedReplies()
            parsedReplies
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun insertParsedReplies(parsedReplies: List<DatabaseParsedReply>) {
        parsedReplyDao.insertParsedReplies(parsedReplies)
    }

    fun insertParsedReply(parsedReply: DatabaseParsedReply) {
        parsedReplyDao.insertParsedReply(parsedReply)
    }

    fun deleteParsedReply(parsedReply: DatabaseParsedReply) {
        parsedReplyDao.deleteParsedReply(parsedReply)
    }
}