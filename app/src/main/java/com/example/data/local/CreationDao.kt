package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreationDao {
    @Query("SELECT * FROM creations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CreationEntity>>

    @Query("SELECT * FROM creations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CreationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(creation: CreationEntity): Long

    @Query("UPDATE creations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE creations SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)

    @Query("DELETE FROM creations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
