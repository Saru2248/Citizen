package com.citizenai.app.data.repository

import android.util.Log
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.data.remote.firebase.FirestoreService
import com.citizenai.app.domain.model.AdminLevel
import com.citizenai.app.domain.model.User
import com.citizenai.app.domain.model.UserRole
import com.citizenai.app.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

private const val TAG = "CitizenAI_Auth"

/**
 * AuthRepositoryImpl — handles user authentication via Firebase Authentication
 * with database profile sync strictly to Cloud Firestore.
 */
class AuthRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
    private val firestoreService: FirestoreService,
    private val firebaseAuth: FirebaseAuth,
    private val complaintDao: com.citizenai.app.data.local.dao.ComplaintDao
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Email and password are required."))
        }

        return try {
            // 1. Authenticate with Firebase Authentication
            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Authentication failed: Empty user profile.")

            val token = runCatching { firebaseUser.getIdToken(true).await()?.token }.getOrNull()
                ?: firebaseUser.uid

            // 2. Retrieve user profile from Cloud Firestore (with 5s timeout) or fallback to authenticated FirebaseUser info
            val fsUser = runCatching {
                kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    firestoreService.getUser(firebaseUser.uid).getOrNull()
                }
            }.getOrNull()

            val user = fsUser ?: User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: trimmedEmail.substringBefore("@"),
                email = trimmedEmail,
                role = UserRole.CITIZEN
            )

            dataStore.saveSession(
                token      = token,
                userId     = user.id,
                userName   = user.name,
                email      = user.email,
                role       = user.role.name,
                adminLevel = user.adminLevel?.name
            )

            runCatching {
                kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    firestoreService.saveUser(user)
                }
            }
            Result.success(user)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Account not found. Please register for a new account."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Incorrect email or password."))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Unable to connect. Please check your internet connection."))
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase Login Exception code: ${e.errorCode}, msg: ${e.message}")
            when (e.errorCode) {
                "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> Result.failure(Exception("Incorrect email or password."))
                "ERROR_USER_DISABLED" -> Result.failure(Exception("You are not authorized to access this account."))
                "ERROR_TOO_MANY_REQUESTS" -> Result.failure(Exception("Too many failed attempts. Please try again later."))
                else -> Result.failure(Exception(e.message ?: "Incorrect email or password."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login exception: ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(Exception(e.message ?: "Login failed. Please try again."))
        }
    }

    override suspend fun register(name: String, email: String, password: String, phone: String): Result<User> {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        return try {
            // 1. Create account with Firebase Authentication
            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Registration failed: Could not create Firebase user account.")

            // 2. Set profile display name
            runCatching {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }

            // 3. Obtain Firebase ID Token
            val token = runCatching { firebaseUser.getIdToken(false).await()?.token }.getOrNull()
                ?: firebaseUser.uid

            // 4. Construct domain User (strictly CITIZEN role)
            val user = User(
                id = firebaseUser.uid,
                name = trimmedName,
                email = trimmedEmail,
                phone = trimmedPhone,
                role = UserRole.CITIZEN,
                adminLevel = null
            )

            // 5. Save session locally
            dataStore.saveSession(
                token      = token,
                userId     = user.id,
                userName   = user.name,
                email      = user.email,
                role       = user.role.name,
                adminLevel = null
            )

            // 6. Save user profile to Cloud Firestore database storage (users/{uid})
            runCatching {
                kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    firestoreService.saveUser(user)
                }
            }

            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account with this email already exists. Please sign in."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Please enter a valid email address."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password is too weak. Please use a stronger password with at least 6 characters."))
        } catch (e: FirebaseNetworkException) {
            Result.failure(Exception("Unable to connect. Please check your internet connection."))
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase Registration Exception code: ${e.errorCode}, msg: ${e.message}")
            val msg = e.message ?: ""
            val mappedMessage = when {
                e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || msg.contains("EMAIL_EXISTS") ->
                    "An account with this email already exists. Please sign in."
                e.errorCode == "ERROR_INVALID_EMAIL" || msg.contains("INVALID_EMAIL") ->
                    "Please enter a valid email address."
                e.errorCode == "ERROR_WEAK_PASSWORD" || msg.contains("WEAK_PASSWORD") ->
                    "Password is too weak. Please use a stronger password."
                e.errorCode == "ERROR_USER_DISABLED" || msg.contains("USER_DISABLED") ->
                    "This account has been disabled."
                e.errorCode == "ERROR_TOO_MANY_REQUESTS" || msg.contains("TOO_MANY_ATTEMPTS") ->
                    "Too many attempts. Please try again later."
                e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || msg.contains("CONFIGURATION_NOT_FOUND") ->
                    "Email/Password authentication is disabled in Firebase Console. Please enable Email/Password under Authentication -> Sign-in method in Firebase Console."
                else -> e.message ?: "Unable to create account. Please try again."
            }
            Result.failure(Exception(mappedMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Registration exception: ${e.javaClass.simpleName}: ${e.message}")
            val msg = e.message ?: ""
            if (msg.contains("CONFIGURATION_NOT_FOUND")) {
                Result.failure(Exception("Email/Password authentication is disabled in Firebase Console. Please enable Email/Password under Authentication -> Sign-in method in Firebase Console."))
            } else if (e is ConnectException || e is UnknownHostException || e is SocketTimeoutException) {
                Result.failure(Exception("Unable to connect. Please check your internet connection."))
            } else {
                Result.failure(Exception(msg.ifBlank { "Unable to create account. Please try again." }))
            }
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val fsUser = firestoreService.getUser(firebaseUser.uid).getOrNull()
                val user = fsUser ?: User(
                    id = firebaseUser.uid,
                    name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                    email = firebaseUser.email ?: "",
                    role = UserRole.CITIZEN
                )
                Result.success(user)
            } else {
                getStoredUser()
            }
        } catch (e: Exception) {
            getStoredUser()
        }
    }

    private suspend fun getStoredUser(): Result<User> {
        val userId    = dataStore.getUserId().firstOrNull()
        val userName  = dataStore.getUserName().firstOrNull()
        val userEmail = dataStore.getUserEmail().firstOrNull()
        val roleStr   = dataStore.getUserRole().firstOrNull()
        val adminLevelStr = dataStore.getAdminLevel().firstOrNull()

        if (userId.isNullOrBlank() || userName.isNullOrBlank() || userEmail.isNullOrBlank() || roleStr.isNullOrBlank()) {
            return Result.failure(Exception("Session expired. Please sign in again."))
        }

        val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.CITIZEN)
        val adminLevel = adminLevelStr?.let { runCatching { AdminLevel.valueOf(it) }.getOrNull() }

        return Result.success(
            User(
                id         = userId,
                name       = userName,
                email      = userEmail,
                role       = role,
                adminLevel = adminLevel
            )
        )
    }

    override suspend fun logout() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: dataStore.getUserId().firstOrNull() ?: ""
        if (currentUserId.isNotBlank()) {
            runCatching { complaintDao.deleteByCitizenId(currentUserId) }
        }
        runCatching { firebaseAuth.signOut() }
        dataStore.clearSession()
    }

    override fun getStoredUserRole(): Flow<UserRole?> =
        dataStore.getUserRole().map { roleStr ->
            roleStr?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
        }

    override fun isAuthenticated(): Flow<Boolean> = dataStore.isAuthenticated()
}
