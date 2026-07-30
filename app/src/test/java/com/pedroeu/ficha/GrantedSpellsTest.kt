package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.SpeciesData
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.content.SpellGrantData
import com.pedroeu.ficha.data.content.SubclassData
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.CharacterSpells
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the bug where a spell granted outright — a Cleric's domain list, an
 * Artificer's Mending, a dragonmark's free casting — never reached the sheet, because only
 * spells the player picked from a list were ever collected.
 */
class GrantedSpellsTest {

    private fun character(
        classId: String,
        level: Int,
        subclassId: String? = null,
        speciesId: String = "human",
        lineageId: String? = null,
        featIds: List<String> = emptyList(),
        knownSpells: List<KnownSpell> = emptyList(),
    ) = PlayerCharacter(
        id = "t",
        name = "Test",
        speciesId = speciesId,
        lineageId = lineageId,
        classId = classId,
        subclassId = subclassId,
        backgroundId = "soldier",
        level = level,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        featIds = featIds,
        knownSpells = knownSpells,
    )

    private fun spellNames(character: PlayerCharacter) =
        CharacterSpells.all(character).map { it.name }

    // ------------------------------------------------------------- The reported bug

    @Test
    fun `a class that simply knows a cantrip has it on the sheet`() {
        // Tinker's Magic: "You know the Mending cantrip." No pick involved.
        assertTrue(spellNames(character("artificer", level = 1)).contains("Mending"))
    }

    @Test
    fun `a cleric's domain spells appear without being chosen`() {
        val cleric = character("cleric", level = 3, subclassId = "life_domain")
        val names = spellNames(cleric)

        assertTrue("Bless is a Life Domain spell", names.contains("Bless"))
        assertTrue("Cure Wounds is a Life Domain spell", names.contains("Cure Wounds"))
        assertTrue("Aid is a Life Domain spell", names.contains("Aid"))
    }

    @Test
    fun `a warlock's patron spells appear without being chosen`() {
        val warlock = character("warlock", level = 3, subclassId = "fiend")
        val names = spellNames(warlock)

        assertTrue(names.contains("Burning Hands"))
        assertTrue(names.contains("Command"))
        assertTrue("Eldritch Blast comes from the class itself", names.contains("Eldritch Blast"))
    }

    @Test
    fun `a species trait that grants a cantrip puts it on the sheet`() {
        val khoravar = character("fighter", level = 1, speciesId = "khoravar")
        assertTrue(spellNames(khoravar).contains("Friends"))
    }

    @Test
    fun `a lineage that grants spells puts them on the sheet`() {
        val drow = character("rogue", level = 5, speciesId = "elf", lineageId = "drow")
        val names = spellNames(drow)

        assertTrue(names.contains("Dancing Lights"))
        assertTrue("Faerie Fire arrives at level 3", names.contains("Faerie Fire"))
        assertTrue("Darkness arrives at level 5", names.contains("Darkness"))
    }

    @Test
    fun `a feat that grants a spell puts it on the sheet`() {
        val marked = character("rogue", level = 3, featIds = listOf("mark_of_shadow"))
        val names = spellNames(marked)

        assertTrue(names.contains("Minor Illusion"))
        assertTrue(names.contains("Invisibility"))
    }

    @Test
    fun `a background's feat grants its spells through the feat`() {
        // Moonwell Pilgrim grants Magic Initiate (Druid); Fey-Touched is the same shape.
        val touched = character("fighter", level = 4, featIds = listOf("fey_touched"))
        assertTrue(spellNames(touched).contains("Misty Step"))
    }

    // ------------------------------------------------------------- Level gating

    @Test
    fun `grants wait for the level that unlocks them`() {
        val early = spellNames(character("cleric", level = 3, subclassId = "life_domain"))
        assertFalse("Revivify is a level 5 domain spell", early.contains("Revivify"))

        val later = spellNames(character("cleric", level = 5, subclassId = "life_domain"))
        assertTrue(later.contains("Revivify"))
    }

    @Test
    fun `a drow below level 3 has only the starting cantrip`() {
        val young = spellNames(character("rogue", level = 1, speciesId = "elf", lineageId = "drow"))
        assertTrue(young.contains("Dancing Lights"))
        assertFalse(young.contains("Faerie Fire"))
    }

    // ------------------------------------------------------------- Merging behaviour

    @Test
    fun `spells the player chose are kept alongside the granted ones`() {
        val chosen = KnownSpell(
            id = "fire_bolt", name = "Fire Bolt", level = 0, school = "Evocation",
            description = "A mote of fire.", source = "Wizard",
        )
        val artificer = character("artificer", level = 1, knownSpells = listOf(chosen))
        val names = spellNames(artificer)

        assertTrue("the chosen spell survives", names.contains("Fire Bolt"))
        assertTrue("the granted spell is added", names.contains("Mending"))
    }

    @Test
    fun `a spell that is both granted and stored is listed once`() {
        val duplicate = KnownSpell(
            id = "mending", name = "Mending", level = 0, school = "Transmutation",
            description = "Repairs an object.", source = "Added by hand",
        )
        val artificer = character("artificer", level = 1, knownSpells = listOf(duplicate))

        assertEquals(1, spellNames(artificer).count { it == "Mending" })
    }

    @Test
    fun `granted spells are marked so the sheet won't offer to delete them`() {
        val artificer = character("artificer", level = 1)
        assertTrue(CharacterSpells.isGranted(artificer, "mending"))
        assertFalse(CharacterSpells.isGranted(artificer, "fire_bolt"))
    }

    @Test
    fun `granted spells carry the source that explains where they came from`() {
        val cleric = character("cleric", level = 3, subclassId = "life_domain")
        val bless = CharacterSpells.granted(cleric).first { it.spell.id == "bless" }

        assertEquals("Life Domain", bless.source)
        assertTrue("domain spells are always prepared", bless.alwaysPrepared)
    }

    @Test
    fun `a character with no granting sources is unaffected`() {
        val fighter = character("fighter", level = 5, subclassId = "champion")
        assertTrue(CharacterSpells.granted(fighter).isEmpty())
        assertTrue(spellNames(fighter).isEmpty())
    }

    // ------------------------------------------------------------- Data integrity

    @Test
    fun `every granted spell id resolves to a real spell`() {
        val everySubclass: List<String?> = listOf(null) + SubclassData.ALL.map { it.id }
        val unresolved = sortedSetOf<String>()

        ClassData.ALL.forEach { charClass ->
            everySubclass.forEach { subclassId ->
                SpeciesData.ALL.forEach { species ->
                    val lineages: List<String?> =
                        listOf(null) + species.lineageOptions.map { it.id }
                    lineages.forEach { lineageId ->
                        val pc = character(
                            classId = charClass.id,
                            level = 20,
                            subclassId = subclassId,
                            speciesId = species.id,
                            lineageId = lineageId,
                            featIds = FeatData.ALL.map { it.id },
                        )
                        unresolved.addAll(CharacterSpells.unresolvedGrants(pc))
                    }
                }
            }
        }

        assertTrue(
            "these grants name spells the catalog doesn't have: $unresolved",
            unresolved.isEmpty(),
        )
    }

    @Test
    fun `every grant table key matches a real class, subclass, species, lineage, or feat`() {
        // A typo here is silent — the map entry simply never fires — which is exactly how the
        // Cleric domains ended up granting nothing.
        val classIds = ClassData.ALL.map { it.id }.toSet()
        val subclassIds = SubclassData.ALL.map { it.id }.toSet()
        val speciesIds = SpeciesData.ALL.map { it.id }.toSet()
        val lineageIds = SpeciesData.ALL.flatMap { s -> s.lineageOptions.map { it.id } }.toSet()
        val featIds = FeatData.ALL.map { it.id }.toSet()
        val known = classIds + subclassIds + speciesIds + lineageIds + featIds

        val unknown = SpellGrantData.sourceIds() - known
        assertTrue("these grant keys match nothing in the rulebook data: $unknown", unknown.isEmpty())
    }

    @Test
    fun `the spell catalog has no duplicate ids`() {
        val ids = SpellData.ALL.map { it.id }
        assertEquals("duplicate spell ids", ids.distinct().size, ids.size)
    }
}
