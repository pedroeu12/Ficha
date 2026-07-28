package com.pedroeu.ficha.ui.sheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.model.InventoryItem
import com.pedroeu.ficha.domain.CharacterCalculations
import com.pedroeu.ficha.domain.Coins
import com.pedroeu.ficha.domain.DeathSaves
import com.pedroeu.ficha.domain.PlayerCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SheetViewModel(
    private val repository: CharacterRepository,
    private val characterId: String,
) : ViewModel() {

    private val _character = MutableStateFlow<PlayerCharacter?>(null)
    val character: StateFlow<PlayerCharacter?> = _character.asStateFlow()

    init {
        viewModelScope.launch {
            _character.value = repository.getById(characterId)
        }
    }

    private fun update(transform: (PlayerCharacter) -> PlayerCharacter) {
        val current = _character.value ?: return
        val updated = transform(current)
        _character.value = updated
        viewModelScope.launch { repository.save(updated) }
    }

    fun adjustHitPoints(delta: Int) = update { character ->
        val max = CharacterCalculations.maxHitPoints(character)
        if (delta < 0) {
            // Damage burns through temporary hit points first.
            val absorbed = minOf(character.temporaryHitPoints, -delta)
            character.copy(
                temporaryHitPoints = character.temporaryHitPoints - absorbed,
                currentHitPoints = (character.currentHitPoints + delta + absorbed).coerceIn(0, max),
            )
        } else {
            character.copy(currentHitPoints = (character.currentHitPoints + delta).coerceIn(0, max))
        }
    }

    fun setTemporaryHitPoints(value: Int) = update {
        it.copy(temporaryHitPoints = value.coerceAtLeast(0))
    }

    fun setHitDiceSpent(value: Int) = update { character ->
        character.copy(hitDiceSpent = value.coerceIn(0, character.level))
    }

    fun setDeathSaves(successes: Int, failures: Int) = update {
        it.copy(
            deathSaves = DeathSaves(
                successes = successes.coerceIn(0, 3),
                failures = failures.coerceIn(0, 3),
            )
        )
    }

    fun toggleHeroicInspiration() = update {
        it.copy(heroicInspiration = !it.heroicInspiration)
    }

    fun toggleEquipped(index: Int) = update { character ->
        val items = character.inventory.toMutableList()
        val item = items.getOrNull(index) ?: return@update character
        items[index] = item.copy(equipped = !item.equipped)
        character.copy(inventory = items)
    }

    fun addInventoryItem(name: String, quantity: Int) = update { character ->
        if (name.isBlank()) return@update character
        character.copy(
            inventory = character.inventory + InventoryItem(name = name.trim(), quantity = quantity)
        )
    }

    fun removeInventoryItem(index: Int) = update { character ->
        val items = character.inventory.toMutableList()
        if (index !in items.indices) return@update character
        items.removeAt(index)
        character.copy(inventory = items)
    }

    fun setCoins(coins: Coins) = update { it.copy(coins = coins) }

    fun setSpellSlotsExpended(level: Int, expended: Int) = update { character ->
        character.copy(
            spellSlotsExpended = character.spellSlotsExpended + (level.toString() to expended.coerceAtLeast(0))
        )
    }

    fun setNotes(value: String) = update { it.copy(notes = value) }
    fun setAppearance(value: String) = update { it.copy(appearance = value) }
    fun setBackstory(value: String) = update { it.copy(backstory = value) }

    fun longRest() = update { character ->
        character.copy(
            currentHitPoints = CharacterCalculations.maxHitPoints(character),
            temporaryHitPoints = 0,
            hitDiceSpent = (character.hitDiceSpent - maxOf(1, character.level / 2)).coerceAtLeast(0),
            deathSaves = DeathSaves(),
            spellSlotsExpended = emptyMap(),
        )
    }

    class Factory(
        private val repository: CharacterRepository,
        private val characterId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SheetViewModel(repository, characterId) as T
    }
}
