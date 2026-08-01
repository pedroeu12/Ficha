package com.pedroeu.ficha.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    fun observeById(id: String): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: String): CharacterEntity?

    @Query("SELECT * FROM characters ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CharacterEntity>

    @Upsert
    suspend fun upsert(entity: CharacterEntity)

    @Delete
    suspend fun delete(entity: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: String)
}
