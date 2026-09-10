package com.citizenai.app.domain.usecase.complaint

import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.repository.ComplaintRepository
import javax.inject.Inject

class GetComplaintDetailUseCase @Inject constructor(
    private val complaintRepository: ComplaintRepository
) {
    suspend operator fun invoke(id: String): Result<Complaint> =
        complaintRepository.getComplaintById(id)
}
