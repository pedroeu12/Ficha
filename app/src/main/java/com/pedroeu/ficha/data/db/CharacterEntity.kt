package com.pedroeu.ficha.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val speciesId: String,
    val classId: String,
    val level: Int,
    val updatedAt: Long,
    /** The full PlayerCharacter serialized as JSON. */
    val payload: String,
)
