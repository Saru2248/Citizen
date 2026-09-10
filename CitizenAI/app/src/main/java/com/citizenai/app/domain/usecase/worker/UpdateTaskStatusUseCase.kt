package com.citizenai.app.domain.usecase.worker

import com.citizenai.app.domain.model.TaskStatus
import com.citizenai.app.domain.repository.WorkerRepository
import javax.inject.Inject

class UpdateTaskStatusUseCase @Inject constructor(
    private val workerRepository: WorkerRepository
) {
    suspend operator fun invoke(taskId: String, status: TaskStatus): Result<Unit> =
        workerRepository.updateTaskStatus(taskId, status)
}
