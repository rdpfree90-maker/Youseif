package com.youseif.playerpro.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.youseif.playerpro.data.model.Source
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY name ASC")
    fun getAll(): Flow<List<Source>>

    @Query("SELECT * FROM sources WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<Source>>

    @Query("SELECT * FROM sources WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 50")
    fun getRecent(): Flow<List<Source>>

    @Query("SELECT * FROM sources WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<Source>>

    @Query("SELECT DISTINCT category FROM sources WHERE category != '' ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query(
        """
        SELECT * FROM sources 
        WHERE name LIKE '%' || :query || '%' 
           OR url LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun search(query: String): Flow<List<Source>>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getById(id: Long): Source?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: Source): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<Source>)

    @Update
    suspend fun update(source: Source)

    @Delete
    suspend fun delete(source: Source)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sources SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE sources SET lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun count(): Int
}
