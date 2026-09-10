package com.citizenai.app.domain.repository

import com.citizenai.app.domain.model.User
import com.citizenai.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String, phone: String): Result<User>
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout()
    fun getStoredUserRole(): Flow<UserRole?>
    fun isAuthenticated(): Flow<Boolean>
}
