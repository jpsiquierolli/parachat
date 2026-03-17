package parachat.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val status: String = UserStatus.OFFLINE.name,
    val about: String = "",
    val lastSeen: Long = 0L
)
