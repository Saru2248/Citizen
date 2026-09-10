package com.citizenai.app.domain.usecase.worker

import com.citizenai.app.domain.repository.WorkerRepository
import java.io.File
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val workerRepository: WorkerRepository
) {
    suspend operator fun invoke(
        taskId: String,
        afterImageFile: File,
        notes: String
    ): Result<Unit> {
        if (!afterImageFile.exists()) {
            return Result.failure(IllegalArgumentException("After photo is required to complete the task"))
        }
        return workerRepository.completeTask(taskId, afterImageFile, notes)
    }
}
