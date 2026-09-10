package com.citizenai.app.domain.usecase.auth

import com.citizenai.app.domain.model.User
import com.citizenai.app.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Email is required"))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Password is required"))
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        return authRepository.login(email.trim(), password)
    }
}
