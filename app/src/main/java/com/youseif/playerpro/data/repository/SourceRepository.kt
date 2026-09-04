package com.youseif.playerpro.data.repository

import com.youseif.playerpro.data.local.SourceDao
import com.youseif.playerpro.data.model.Source
import kotlinx.coroutines.flow.Flow

class SourceRepository(private val dao: SourceDao) {

    fun getAll(): Flow<List<Source>> = dao.getAll()
    fun getFavorites(): Flow<List<Source>> = dao.getFavorites()
    fun getRecent(): Flow<List<Source>> = dao.getRecent()
    fun getByCategory(category: String): Flow<List<Source>> = dao.getByCategory(category)
    fun getCategories(): Flow<List<String>> = dao.getCategories()
    fun search(query: String): Flow<List<Source>> = dao.search(query)

    suspend fun getById(id: Long): Source? = dao.getById(id)

    suspend fun add(source: Source): Long = dao.insert(
        source.copy(
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun update(source: Source) = dao.update(
        source.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun delete(source: Source) = dao.delete(source)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun markPlayed(id: Long) = dao.updateLastPlayed(id, System.currentTimeMillis())

    suspend fun insertAll(sources: List<Source>) = dao.insertAll(sources)

    suspend fun count(): Int = dao.count()
}
