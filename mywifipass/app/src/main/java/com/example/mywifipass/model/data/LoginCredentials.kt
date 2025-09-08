package app.mywifipass.model.data

data class LoginCredentials(
    var url: String = "",
    var login: String = "",
    var pwd: String = "",
    var usePassword: Boolean = true
) {
    fun isNotEmpty(): Boolean {
        return login.isNotEmpty() && pwd.isNotEmpty() && url.isNotEmpty()
    }
}