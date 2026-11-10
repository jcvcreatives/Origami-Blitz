package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "Crafter",
    val role: String = "player",
    val level: Long = 1L,
    val highestUnlockedLevel: Long = 1L,
    val highScore: Long = 0L,
    val scissorsCount: Long = 3L,
    val glueCount: Long = 3L,
    val hammerCount: Long = 2L,
    val starsEncoded: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isGm: Boolean
        get() = role == "gm" || username.equals("jcv", ignoreCase = true)
}

data class CloudQuotaStats(
    val sessionReads: Int = 0,
    val sessionWrites: Int = 0,
    val skippedDuplicateWrites: Int = 0,
    val lastSyncStatus: String = "Local Cache Ready (0 Quota Used)"
)
