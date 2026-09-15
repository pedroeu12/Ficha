package com.pedroeu.ficha

import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.db.CharacterDao
import com.pedroeu.ficha.data.db.CharacterEntity
import com.pedroeu.ficha.domain.PlayerCharacter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * The character store, in memory.
 *
 * The screens do not take a character, they take a repository and an id, because that is how
 * the app hands them one — and until a test could supply that, no test could open a screen.
 * Room's own in-memory database would work on a device; this works on the JVM, in the same
 * milliseconds as every other test, and is the whole reason [SheetScreenTest] exists.
 */
class FakeCharacterDao : CharacterDao {

    private val rows = MutableStateFlow<Map<String, CharacterEntity>>(emptyMap())

    private fun sorted(map: Map<String, CharacterEntity>) =
        map.values.sortedByDescending { it.updatedAt }

    override fun observeAll(): Flow<List<CharacterEntity>> = rows.map { sorted(it) }

    override fun observeById(id: String): Flow<CharacterEntity?> = rows.map { it[id] }

    override suspend fun getById(id: String): CharacterEntity? = rows.value[id]

    override suspend fun getAll(): List<CharacterEntity> = sorted(rows.value)

    override suspend fun upsert(entity: CharacterEntity) {
        rows.value = rows.value + (entity.id to entity)
    }

    override suspend fun delete(entity: CharacterEntity) {
        rows.value = rows.value - entity.id
    }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value - id
    }
}

/** A repository already holding [characters], ready to be handed to a screen. */
fun repositoryHolding(vararg characters: PlayerCharacter): CharacterRepository {
    val repository = CharacterRepository(FakeCharacterDao())
    runBlocking { characters.forEach { repository.save(it) } }
    return repository
}
