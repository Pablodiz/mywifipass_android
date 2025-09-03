package app.mywifipass.model.data

data class LoginCredentials(
    var url: String = "",
    var login: String = "",
    var pwd: String = "",
) {
    fun isNotEmpty(): Boolean {
        return login.isNotEmpty() && pwd.isNotEmpty() && url.isNotEmpty()
    }
}