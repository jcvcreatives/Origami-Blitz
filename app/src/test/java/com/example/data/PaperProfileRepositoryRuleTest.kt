package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncAndGetMyProfile_authenticatedOwner_succeedsAndRespectsQuotaGuard() = runBlocking {
        signInTestUser("jcv_owner@test.com")
        val repository = PaperProfileRepository(firestore)

        // 1. Initial create write
        val syncResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyProfile(
                username = "jcv",
                role = "gm",
                level = 12,
                highestUnlockedLevel = 50,
                highScore = 42000,
                scissorsCount = 99,
                glueCount = 99,
                hammerCount = 99,
                starsEncoded = "1:3,2:3,3:3",
                forceImmediate = true
            )
        }
        assertTrue(syncResult.isSuccess)
        assertEquals(true, syncResult.getOrThrow())

        // 2. Redundant identical non-forced sync is skipped by Quota Guard (returns false, 0 network writes)
        val duplicateSync = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyProfile(
                username = "jcv",
                role = "gm",
                level = 12,
                highestUnlockedLevel = 50,
                highScore = 42000,
                scissorsCount = 99,
                glueCount = 99,
                hammerCount = 99,
                starsEncoded = "1:3,2:3,3:3",
                forceImmediate = false
            )
        }
        assertTrue(duplicateSync.isSuccess)
        assertEquals(false, duplicateSync.getOrThrow())

        // 3. Read back profile
        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getMyProfile()
        }
        assertTrue(profileResult.isSuccess)
        val profile = profileResult.getOrThrow()
        assertNotNull(profile)
        assertEquals("jcv", profile?.username)
        assertEquals("gm", profile?.role)
        assertEquals(50L, profile?.highestUnlockedLevel)
    }

    @Test
    fun observeMyProfile_authenticatedOwner_emitsRealtimeProfile() = runBlocking {
        signInTestUser("alice_player@test.com")
        val repository = PaperProfileRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncMyProfile(
                username = "AliceCrafter",
                role = "player",
                level = 3,
                highestUnlockedLevel = 5,
                highScore = 8400,
                scissorsCount = 4,
                glueCount = 3,
                hammerCount = 2,
                starsEncoded = "1:3,2:2",
                forceImmediate = true
            ).getOrThrow()
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMyProfile().first { it != null && it.username == "AliceCrafter" }
        }
        assertNotNull(observed)
        assertEquals(8400L, observed?.highScore)
    }

    @Test
    fun getProfileById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_isolated@test.com")
        val aliceRepo = PaperProfileRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.syncMyProfile(
                username = "Alice",
                role = "player",
                level = 2,
                highestUnlockedLevel = 2,
                highScore = 1500,
                scissorsCount = 3,
                glueCount = 3,
                hammerCount = 2,
                starsEncoded = "1:3",
                forceImmediate = true
            ).getOrThrow()
        }

        signInTestUser("bob_intruder@test.com")
        val bobRepo = PaperProfileRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProfileById(aliceUid)
        }
        assertTrue(crossReadResult.isFailure)
        val firestoreEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeMyProfile_unauthenticatedUser_failsFast() = runBlocking {
        auth.signOut()
        val repository = PaperProfileRepository(firestore)

        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMyProfile().first()
            }
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
