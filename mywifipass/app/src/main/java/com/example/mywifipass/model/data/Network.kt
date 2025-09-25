package app.mywifipass.model.data

import kotlinx.serialization.*
import androidx.room.Entity
import androidx.room.PrimaryKey

@Serializable
@Entity(tableName = "networks")
data class Network(
    @PrimaryKey(autoGenerate = true)
    @Transient
    val id: Int = 0,
    
    @SerialName("email")
    val user_email: String,
    
    val network_common_name: String, 
    val ssid: String,
    val location: String="",
    val start_date: String,
    val end_date: String,
    val description: String="",
    val location_name: String,
    val validation_url: String,
    val certificates_url: String,
    var has_downloaded_url: String,
    var check_user_authorized_url: String,
    val certificates_symmetric_key: String,
    var is_user_authorized: Boolean,

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