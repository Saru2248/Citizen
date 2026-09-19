package com.citizenai.app.data.local.dao

import androidx.room.*
import com.citizenai.app.data.local.entity.ComplaintEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY reportedAt DESC")
    fun observeAll(): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints ORDER BY reportedAt DESC")
    suspend fun getAll(): List<ComplaintEntity>

    @Query("SELECT * FROM complaints WHERE citizenId = :citizenId ORDER BY reportedAt DESC")
    fun observeByCitizenId(citizenId: String): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE citizenId = :citizenId ORDER BY reportedAt DESC")
    suspend fun getByCitizenId(citizenId: String): List<ComplaintEntity>

    @Query("SELECT * FROM complaints WHERE id = :id")
    suspend fun getById(id: String): ComplaintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(complaints: List<ComplaintEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(complaint: ComplaintEntity)

    @Query("DELETE FROM complaints WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM complaints")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM complaints WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM complaints WHERE status = 'RESOLVED' OR status = 'COMPLETED'")
    suspend fun getResolvedCount(): Int
}
