/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

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