package com.example.parachat.domain

fun User.displayName(): String = displayNameFromParts(username = username, email = email, id = id)

fun displayNameFromParts(username: String?, email: String, id: String): String {
    val normalizedUsername = username?.trim().orEmpty()
    if (normalizedUsername.isNotBlank()) return normalizedUsername

    val emailPrefix = email.substringBefore("@").trim()
    if (emailPrefix.isNotBlank()) return emailPrefix

    val shortId = id.trim().take(6)
    return if (shortId.isNotBlank()) "user_$shortId" else "User"
}
