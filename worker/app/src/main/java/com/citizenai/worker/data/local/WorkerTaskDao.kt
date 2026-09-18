package com.citizenai.worker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerTaskDao {

    @Query("SELECT * FROM worker_tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<WorkerTaskEntity>>

    @Query("SELECT * FROM worker_tasks WHERE id = :id OR complaintId = :id LIMIT 1")
    suspend fun getTaskById(id: String): WorkerTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<WorkerTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: WorkerTaskEntity)

    @Query("DELETE FROM worker_tasks")
    suspend fun clearAll()
}
