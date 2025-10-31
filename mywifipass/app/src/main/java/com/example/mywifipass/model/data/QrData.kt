/*
 * BSD 3-Clause License
 * Copyright (c) 2025, Pablo Diz de la Cruz
 * All rights reserved.
 *
 * This file is licensed under the BSD 3-Clause License.
 * For full license text, see the LICENSE file in the root directory of this project.
 */

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