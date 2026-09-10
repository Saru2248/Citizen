package com.citizenai.app.domain.usecase.complaint

import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.repository.ComplaintRepository
import javax.inject.Inject

class GetMyComplaintsUseCase @Inject constructor(
    private val complaintRepository: ComplaintRepository
) {
    suspend operator fun invoke(): Result<List<Complaint>> =
        complaintRepository.getMyComplaints()
}
