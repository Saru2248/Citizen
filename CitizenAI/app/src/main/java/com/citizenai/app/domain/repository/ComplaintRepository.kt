package com.citizenai.app.domain.repository

import com.citizenai.app.domain.model.Comment
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.ComplaintStatus
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

interface ComplaintRepository {
    /** Fetch all complaints for the current citizen */
    suspend fun getMyComplaints(): Result<List<Complaint>>

    /** Get a single complaint by its internal ID */
    suspend fun getComplaintById(id: String): Result<Complaint>

    /** Get the timeline events for a complaint */
    suspend fun getComplaintTimeline(complaintId: String): Result<List<TimelineEvent>>

    /** Get comments on a complaint */
    suspend fun getComments(complaintId: String): Result<List<Comment>>

    /** Post a comment */
    suspend fun postComment(complaintId: String, message: String): Result<Comment>

    /** Submit a new complaint (multipart — includes image upload) */
    suspend fun submitComplaint(
        imagePath: String,
        category: IssueCategory,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String,
        issueType: String,
        priority: String,
        department: String
    ): Result<Complaint>

    /** Citizen verifies resolution */
    suspend fun verifyResolution(complaintId: String, isResolved: Boolean, reason: String?): Result<Unit>

    /** Observe cached complaints from Room (offline support) */
    fun observeCachedComplaints(): Flow<List<Complaint>>

    /** Get all complaints for the map view */
    suspend fun getAllComplaintsForMap(): Result<List<Complaint>>
}
