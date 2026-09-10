package com.citizenai.worker.domain.model

data class StatusHistoryItem(
    val status: String,
    val message: String? = null,
    val note: String? = null,
    val updatedBy: String? = null,
    val actorType: String? = null,
    val actorId: String? = null,
    val timestamp: String? = null
)
