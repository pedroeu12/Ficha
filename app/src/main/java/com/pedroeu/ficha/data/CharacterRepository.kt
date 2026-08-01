package com.pedroeu.ficha.data

import com.pedroeu.ficha.data.db.CharacterDao
import com.pedroeu.ficha.data.db.CharacterEntity
import com.pedroeu.ficha.domain.PlayerCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class CharacterRepository(private val dao: CharacterDao) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun observeAll(): Flow<List<PlayerCharacter>> =
        dao.observeAll().map { list -> list.map { decode(it) } }

    fun observeById(id: String): Flow<PlayerCharacter?> =
        dao.observeById(id).map { entity -> entity?.let { decode(it) } }

    suspend fun getById(id: String): PlayerCharacter? = dao.getById(id)?.let { decode(it) }

    suspend fun save(character: PlayerCharacter) {
        val stamped = character.copy(updatedAt = System.currentTimeMillis())
        dao.upsert(
            CharacterEntity(
                id = stamped.id,
                name = stamped.name,
                speciesId = stamped.speciesId,
                classId = stamped.classId,
                level = stamped.level,
                updatedAt = stamped.updatedAt,
                payload = json.encodeToString(PlayerCharacter.serializer(), stamped),
            )
        )
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    /** Every character, read once rather than observed, for writing a backup. */
    suspend fun getAll(): List<PlayerCharacter> = dao.getAll().map { decode(it) }

    /**
     * Writes restored characters back.
     *
     * The stored timestamp is kept rather than stamped with now, because it is what decides
     * whether a later restore counts as newer. Saving normally would make every restored
     * character look freshly edited and defeat that comparison.
     */
    suspend fun restore(characters: List<PlayerCharacter>) {
        characters.forEach { character ->
            dao.upsert(
                CharacterEntity(
                    id = character.id,
                    name = character.name,
                    speciesId = character.speciesId,
                    classId = character.classId,
                    level = character.level,
                    updatedAt = character.updatedAt,
                    payload = json.encodeToString(PlayerCharacter.serializer(), character),
                )
            )
        }
    }

    private fun decode(entity: CharacterEntity): PlayerCharacter =
        json.decodeFromString(PlayerCharacter.serializer(), entity.payload)
}
