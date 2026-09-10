package com.citizenai.app.domain.model

import java.time.Instant

/**
 * AuditLog — record of critical administrative and system actions.
 */
data class AuditLog(
    val id: String,
    val userId: String,
    val userName: String,
    val userRole: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val oldValue: String? = null,
    val newValue: String? = null,
    val timestamp: Instant = Instant.now()
)
