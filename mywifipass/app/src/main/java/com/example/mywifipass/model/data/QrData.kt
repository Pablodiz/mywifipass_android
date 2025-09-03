package app.mywifipass.model.data

import kotlinx.serialization.Serializable

@Serializable
data class QrData(
    val validation_url: String,
){
    fun toJson(): String {
        return """
            {
                "validation_url": "$validation_url"
            }
        """.trimIndent()
    }
    fun toBodyPetition(): String {
        return toJson()
    }
}