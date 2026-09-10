package com.citizenai.app.domain.usecase.auth

import com.citizenai.app.domain.model.User
import com.citizenai.app.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        phone: String
    ): Result<User> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Full Name is required"))
        }
        if (trimmedName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter a valid full name"))
        }
        if (trimmedEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email address is required"))
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password is required"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        return authRepository.register(trimmedName, trimmedEmail, password, trimmedPhone)
    }
}

