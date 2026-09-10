package com.citizenai.app.domain.usecase.complaint

import com.citizenai.app.domain.repository.ComplaintRepository
import javax.inject.Inject

class VerifyResolutionUseCase @Inject constructor(
    private val complaintRepository: ComplaintRepository
) {
    suspend operator fun invoke(
        complaintId: String,
        isResolved: Boolean,
        reason: String? = null
    ): Result<Unit> {
        if (!isResolved && reason.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Please explain why the issue is not resolved"))
        }
        return complaintRepository.verifyResolution(complaintId, isResolved, reason)
    }
}
