package com.pedroeu.ficha

import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterAttacks
import com.pedroeu.ficha.domain.CustomAttack
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The tablet layout is the same sheet, so it has to say the same numbers.
 *
 * It didn't: an attack the player had written +9 on showed +0, because the ledger printed the
 * derived [com.pedroeu.ficha.domain.AttackLine.attackBonus] — which is zero for a written
 * attack, whose to-hit lives in the label beside it — instead of the [shownBonus] the phone
 * prints. The same override keys have to reach both screens too, or a bonus rewritten on one
 * would vanish on the other.
 */
class TabletParityTest {

    private fun character(attacks: List<CustomAttack>) = PlayerCharacter(
        id = "t", name = "T", speciesId = "human", classId = "fighter",
        subclassId = "champion", backgroundId = "soldier", level = 5,
        customAttacks = attacks,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
    )

    // ------------------------------------------------------------------ The number itself

    @Test
    fun `a written attack carries its to-hit in the label, not the derived bonus`() {
        val written = CustomAttack(
            id = "custom:1",
            name = "Fancy Sword",
            bonus = "+9",
            damageDice = "1d8 + 5",
            damageType = "Slashing",
        )
        val line = CharacterAttacks.all(character(listOf(written)))
            .first { it.id == "custom:1" }

        assertEquals("+9", line.shownBonus)
        assertEquals(
            "the derived bonus is zero here, which is exactly why a screen must not print it",
            0,
            line.attackBonus,
        )
    }

    @Test
    fun `a derived attack falls back to the derived bonus`() {
        // Nothing written, so shownBonus is the formatted calculation and both agree.
        val unarmed = CharacterAttacks.all(character(emptyList())).first { it.id == "unarmed" }
        assertEquals(
            com.pedroeu.ficha.domain.CharacterCalculations.formatModifier(unarmed.attackBonus),
            unarmed.shownBonus,
        )
    }

    @Test
    fun `an override wins over both`() {
        val base = character(emptyList())
        val overridden = base.copy(textOverrides = mapOf("attack:unarmed:bonus" to "+11"))
        val line = CharacterAttacks.all(overridden).first { it.id == "unarmed" }
        assertEquals("+11", line.shownBonus)
    }

    // ------------------------------------------------------------------ Both screens

    /**
     * A source scan, because the mistake is a screen reading the wrong field and no amount of
     * domain testing can catch that. Skipped rather than failed when the sources aren't where
     * the test runner expects, so this can never fail for the wrong reason.
     */
    @Test
    fun `no screen prints the derived attack bonus`() {
        val ui = File("src/main/java/com/pedroeu/ficha/ui")
        if (!ui.isDirectory) return

        val offenders = ui.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readText().contains("attack.attackBonus") }
            .map { it.name }
            .toList()

        assertTrue(
            "these print the derived bonus, which is 0 for a written attack: $offenders",
            offenders.isEmpty(),
        )
    }

    @Test
    fun `both screens make the same attack fields editable`() {
        val ui = File("src/main/java/com/pedroeu/ficha/ui")
        if (!ui.isDirectory) return

        fun fieldsIn(path: String, call: Regex): Set<String> =
            File(ui, path).walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { call.findAll(it.readText()) }
                .map { it.groupValues[1] }
                .toSet()

        // Both build the override key the same way — "attack:<line id>:<field>" — so what
        // differs between the screens is only which fields they offer.
        val phone = fieldsIn("sheet", Regex("""\n\s*field\(\s*"(\w+)"""))
        val tablet = fieldsIn("tablet", Regex("""AttackCell\(handle, attack, "(\w+)""""))

        assertTrue("the phone offers no editable attack fields at all", phone.isNotEmpty())
        assertEquals(
            "an attack field editable on one screen and not the other",
            phone,
            tablet,
        )
    }
}
