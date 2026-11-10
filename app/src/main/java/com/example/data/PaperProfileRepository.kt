package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class PaperProfileRepository(
    private val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth
    private val knownExistingDocs = ConcurrentHashMap<String, Boolean>()
    private val lastSyncedSignatures = ConcurrentHashMap<String, String>()
    private val lastWriteTimestampMs = ConcurrentHashMap<String, Long>()

    private val _quotaStats = MutableStateFlow(CloudQuotaStats())
    val quotaStats: StateFlow<CloudQuotaStats> = _quotaStats.asStateFlow()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun getRemainingCooldownMs(): Long {
        val uid = auth.currentUser?.uid ?: return 0L
        val prevMs = lastWriteTimestampMs[uid] ?: return 0L
        val elapsed = System.currentTimeMillis() - prevMs
        return (MIN_SYNC_INTERVAL_MS - elapsed).coerceAtLeast(0L)
    }

    private fun computeSignature(
        username: String,
        role: String,
        level: Int,
        highestUnlockedLevel: Int,
        highScore: Int,
        scissorsCount: Int,
        glueCount: Int,
        hammerCount: Int,
        starsEncoded: String
    ): String {
        return "$username|$role|$level|$highestUnlockedLevel|$highScore|$scissorsCount|$glueCount|$hammerCount|$starsEncoded"
    }

    /**
     * Fetches the authenticated user's single profile document (1 Read Unit).
     */
    suspend fun getMyProfile(): Result<PlayerProfile?> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            val exists = snapshot.exists()
            knownExistingDocs[uid] = exists
            _quotaStats.update {
                it.copy(
                    sessionReads = it.sessionReads + 1,
                    lastSyncStatus = if (exists) "Loaded profile from Cloud (1 Read)" else "New Cloud profile ready (1 Read)"
                )
            }
            val profile = if (exists) {
                snapshot.toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            if (profile != null) {
                lastSyncedSignatures[uid] = computeSignature(
                    username = profile.username,
                    role = profile.role,
                    level = profile.level.toInt(),
                    highestUnlockedLevel = profile.highestUnlockedLevel.toInt(),
                    highScore = profile.highScore.toInt(),
                    scissorsCount = profile.scissorsCount.toInt(),
                    glueCount = profile.glueCount.toInt(),
                    hammerCount = profile.hammerCount.toInt(),
                    starsEncoded = profile.starsEncoded
                )
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Fetches a specific user's profile by ID (enforced by security rules: owner or admin only).
     */
    suspend fun getProfileById(targetUserId: String): Result<PlayerProfile?> {
        requireUserId()
        val docRef = db.collection("users").document(targetUserId)
        return try {
            val snapshot = docRef.get().await()
            _quotaStats.update { it.copy(sessionReads = it.sessionReads + 1) }
            val profile = if (snapshot.exists()) {
                snapshot.toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Quota-Optimized Real-Time Snapshot Listener for the authenticated user's single profile document.
     * - Uses MetadataChanges.EXCLUDE and skips local pending-write echoes so local updates don't cause read/write loops.
     * - Deduplicates against lastSyncedSignatures so only genuine remote changes from other devices emit downstream.
     */
    fun observeMyProfile(): Flow<PlayerProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots(MetadataChanges.EXCLUDE)
                .filter { snapshot ->
                    // Ignore local optimistic write echoes to prevent ping-pong loops
                    !snapshot.metadata.hasPendingWrites()
                }
                .mapNotNull { snapshot ->
                    val exists = snapshot.exists()
                    knownExistingDocs[uid] = exists
                    if (!exists) {
                        return@mapNotNull null
                    }
                    val profile = snapshot.toObject(
                        PlayerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: return@mapNotNull null

                    val incomingSig = computeSignature(
                        username = profile.username,
                        role = profile.role,
                        level = profile.level.toInt(),
                        highestUnlockedLevel = profile.highestUnlockedLevel.toInt(),
                        highScore = profile.highScore.toInt(),
                        scissorsCount = profile.scissorsCount.toInt(),
                        glueCount = profile.glueCount.toInt(),
                        hammerCount = profile.hammerCount.toInt(),
                        starsEncoded = profile.starsEncoded
                    )

                    // Skip emitting if this snapshot already matches our known synced state
                    if (lastSyncedSignatures[uid] == incomingSig) {
                        return@mapNotNull null
                    }

                    lastSyncedSignatures[uid] = incomingSig
                    _quotaStats.update {
                        it.copy(
                            sessionReads = it.sessionReads + 1,
                            lastSyncStatus = "Synced remote update from Cloud"
                        )
                    }
                    profile
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Quota-Optimized Single-Document Upsert:
     * - Skips write completely if the state signature matches the last synced state (0 Write Units wasted).
     * - Throttles non-forced background syncs to at most once every 8 seconds.
     * - Uses `create` (with `createdAt` + `updatedAt`) for new profiles and `update` (preserving `createdAt`) for existing profiles.
     */
    suspend fun syncMyProfile(
        username: String,
        role: String,
        level: Int,
        highestUnlockedLevel: Int,
        highScore: Int,
        scissorsCount: Int,
        glueCount: Int,
        hammerCount: Int,
        starsEncoded: String,
        forceImmediate: Boolean = false
    ): Result<Boolean> {
        val uid = requireUserId()
        val safeUsername = username.trim().ifEmpty { "Crafter" }.take(40)
        val safeRole = if (role == "gm") "gm" else "player"
        val safeLevel = level.coerceIn(1, 50)
        val safeHighest = highestUnlockedLevel.coerceIn(1, 50)
        val safeHighScore = highScore.coerceIn(0, 100_000_000)
        val safeScissors = scissorsCount.coerceIn(0, 999)
        val safeGlue = glueCount.coerceIn(0, 999)
        val safeHammer = hammerCount.coerceIn(0, 999)
        val safeStars = starsEncoded.take(600)

        val signature = computeSignature(
            username = safeUsername,
            role = safeRole,
            level = safeLevel,
            highestUnlockedLevel = safeHighest,
            highScore = safeHighScore,
            scissorsCount = safeScissors,
            glueCount = safeGlue,
            hammerCount = safeHammer,
            starsEncoded = safeStars
        )

        if (!forceImmediate && lastSyncedSignatures[uid] == signature) {
            _quotaStats.update {
                it.copy(
                    skippedDuplicateWrites = it.skippedDuplicateWrites + 1,
                    lastSyncStatus = "Up to date (Quota Guard skipped redundant write)"
                )
            }
            return Result.success(false)
        }

        val nowMs = System.currentTimeMillis()
        val prevMs = lastWriteTimestampMs[uid] ?: 0L
        if (!forceImmediate && (nowMs - prevMs) < MIN_SYNC_INTERVAL_MS) {
            _quotaStats.update {
                it.copy(
                    skippedDuplicateWrites = it.skippedDuplicateWrites + 1,
                    lastSyncStatus = "Batched in local cache (Quota Cooldown active)"
                )
            }
            return Result.success(false)
        }

        val docRef = db.collection("users").document(uid)
        val isKnownExisting = knownExistingDocs[uid] == true

        val updatePayload = mapOf(
            "username" to safeUsername,
            "role" to safeRole,
            "level" to safeLevel,
            "highestUnlockedLevel" to safeHighest,
            "highScore" to safeHighScore,
            "scissorsCount" to safeScissors,
            "glueCount" to safeGlue,
            "hammerCount" to safeHammer,
            "starsEncoded" to safeStars,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val createPayload = updatePayload + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            if (isKnownExisting) {
                try {
                    docRef.update(updatePayload).await()
                } catch (notFoundEx: FirebaseFirestoreException) {
                    if (notFoundEx.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                        docRef.set(createPayload).await()
                        knownExistingDocs[uid] = true
                    } else {
                        throw notFoundEx
                    }
                }
            } else {
                try {
                    docRef.set(createPayload).await()
                    knownExistingDocs[uid] = true
                } catch (permOrExistEx: FirebaseFirestoreException) {
                    // If document already existed on server prior to local knowledge, update preserving createdAt
                    docRef.update(updatePayload).await()
                    knownExistingDocs[uid] = true
                }
            }
            lastSyncedSignatures[uid] = signature
            lastWriteTimestampMs[uid] = nowMs
            _quotaStats.update {
                it.copy(
                    sessionWrites = it.sessionWrites + 1,
                    lastSyncStatus = "Cloud Synced (1 Write Unit)"
                )
            }
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val opType = if (isKnownExisting) OperationType.UPDATE else OperationType.CREATE
            handleFirestoreError(e, opType, docRef.path)
            Result.failure(e)
        }
    }

    private companion object {
        const val MIN_SYNC_INTERVAL_MS = 8_000L
    }
}
