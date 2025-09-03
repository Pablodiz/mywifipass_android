package app.mywifipass.model.data

import kotlinx.serialization.Serializable

@Serializable
data class QrLoginCredentials(
    var url: String = "",
    var username: String = "",
    var token: String = "",
) {
    fun isNotEmpty(): Boolean {
        return username.isNotEmpty() && token.isNotEmpty() && url.isNotEmpty()
    }
}
