package com.citizenai.app.domain.usecase.worker

import com.citizenai.app.domain.model.WorkerTask
import com.citizenai.app.domain.repository.WorkerRepository
import javax.inject.Inject

class GetWorkerTasksUseCase @Inject constructor(
    private val workerRepository: WorkerRepository
) {
    suspend operator fun invoke(): Result<List<WorkerTask>> =
        workerRepository.getWorkerTasks()
}
