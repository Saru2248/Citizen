package com.citizenai.app.domain.model

import java.time.Instant

data class Comment(
    val id: String,
    val complaintId: String,
    val authorId: String,
    val authorName: String,
    val authorRole: UserRole,
    val message: String,
    val postedAt: Instant
)
