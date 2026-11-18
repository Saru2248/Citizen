package com.citizenai.app.domain.usecase.complaint

import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.repository.ComplaintRepository
import javax.inject.Inject

class SubmitComplaintUseCase @Inject constructor(
    private val complaintRepository: ComplaintRepository
) {
    suspend operator fun invoke(
        imagePath: String,
        category: IssueCategory,
        description: String,
        latitude: Double?,
        longitude: Double?,
        address: String,
        issueType: String,
        priority: String,
        department: String,
        locationSource: String = if (latitude != null && latitude != 0.0 && longitude != null && longitude != 0.0) "GPS" else "MANUAL"
    ): Result<Complaint> {
        // Validation
        if (imagePath.isBlank()) {
            return Result.failure(IllegalArgumentException("Image is required to submit a report"))
        }
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Please add a description"))
        }
        if (address.isBlank()) {
            return Result.failure(IllegalArgumentException("Location is required"))
        }
        if (locationSource.equals("GPS", ignoreCase = true)) {
            if (latitude == null || longitude == null || (latitude == 0.0 && longitude == 0.0)) {
                return Result.failure(IllegalArgumentException("Valid GPS coordinates are required for GPS location"))
            }
        }
        return complaintRepository.submitComplaint(
            imagePath, category, description, latitude, longitude,
            address, issueType, priority, department, locationSource
        )
    }
}
