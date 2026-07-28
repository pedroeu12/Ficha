package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.ClassData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.content.SpellData
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.ui.creation.CreationState
import com.pedroeu.ficha.ui.creation.CreationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's rule is that no grant worded "of your choice" is ever resolved silently.
 * These lock that in, starting with the Sage background that used to skip its spells.
 */
class OriginChoicesTest {

    private fun choicesFor(
        backgroundId: String,
        classId: String = "fighter",
        speciesId: String = "human",
        lineageId: String? = null,
        classSelections: Map<String, List<String>> = emptyMap(),
    ) = OriginChoices.all(speciesId, lineageId, classId, classSelections, backgroundId)

    @Test
    fun `sage presents its magic initiate cantrips and level 1 spell`() {
        val choices = choicesFor("sage")

        val cantrips = choices.find { it.id == "feat:magic_initiate_wizard:cantrips" }
        val spell = choices.find { it.id == "feat:magic_initiate_wizard:spell" }

        assertNotNull("Sage must prompt for Magic Initiate cantrips", cantrips)
        assertNotNull("Sage must prompt for a level 1 spell", spell)
        assertEquals(2, cantrips!!.count)
        assertEquals(1, spell!!.count)
        assertEquals(ChoiceKind.SPELL, cantrips.kind)
        assertEquals(ChoiceKind.SPELL, spell.kind)
    }

    @Test
    fun `sage spell options come from the wizard list at the right levels`() {
        val choices = choicesFor("sage")
        val cantrips = choices.first { it.id == "feat:magic_initiate_wizard:cantrips" }
        val spell = choices.first { it.id == "feat:magic_initiate_wizard:spell" }

        assertTrue(cantrips.options.isNotEmpty())
        cantrips.options.forEach { option ->
            val def = SpellData.byId(option.id)
            assertNotNull("unknown spell ${option.id}", def)
            assertEquals(0, def!!.level)
            assertTrue("${def.name} is not a Wizard spell", "wizard" in def.classes)
        }
        spell.options.forEach { option ->
            val def = SpellData.byId(option.id)!!
            assertEquals(1, def.level)
            assertTrue("wizard" in def.classes)
        }
    }

    @Test
    fun `a sage cannot finish creation without picking those spells`() {
        val base = CreationState(
            step = CreationStep.ORIGIN_CHOICES,
            speciesId = "human",
            classId = "fighter",
            backgroundId = "sage",
        )
        assertFalse("unresolved spell choices must block", base.canAdvance)

        val answered = base.copy(
            originSelections = base.originChoices.associate { choice ->
                choice.id to choice.options.take(choice.count).map { it.id }
            }
        )
        assertTrue(answered.canAdvance)
    }

    @Test
    fun `every magic initiate background prompts for its spells`() {
        val magicInitiateBackgrounds = BackgroundData.ALL.filter {
            it.featId.startsWith("magic_initiate")
        }
        assertTrue(magicInitiateBackgrounds.isNotEmpty())

        magicInitiateBackgrounds.forEach { background ->
            val spellChoices = choicesFor(background.id).filter { it.kind == ChoiceKind.SPELL }
            assertEquals(
                "${background.name} should offer a cantrip choice and a spell choice",
                2,
                spellChoices.size,
            )
        }
    }

    @Test
    fun `backgrounds whose tool names a group prompt for the specific tool`() {
        // Noble's "Gaming Set of your choice" must become a real decision.
        val choice = OriginChoices.forBackgroundTool("noble")
        assertNotNull(choice)
        assertEquals(ChoiceKind.TOOL, choice!!.kind)
        assertTrue(choice.options.size > 1)

        // Acolyte names a specific tool, so there is nothing to ask.
        assertEquals(null, OriginChoices.forBackgroundTool("acolyte"))
    }

    @Test
    fun `high elf is offered a wizard cantrip but other lineages are not`() {
        val highElf = OriginChoices.forLineage("elf", "high_elf")
        assertEquals(1, highElf.size)
        assertEquals(ChoiceKind.SPELL, highElf.first().kind)

        // Wood Elf and Drow are granted a fixed cantrip, so nothing is asked.
        assertTrue(OriginChoices.forLineage("elf", "wood_elf").isEmpty())
        assertTrue(OriginChoices.forLineage("elf", "drow").isEmpty())
    }

    @Test
    fun `class level grants that name a group become choices`() {
        assertTrue(
            "a Bard picks three instruments",
            OriginChoices.forClass("bard", emptyMap()).any { it.kind == ChoiceKind.TOOL },
        )
        assertTrue(
            "a Monk picks a tool or instrument",
            OriginChoices.forClass("monk", emptyMap()).any { it.kind == ChoiceKind.TOOL },
        )
    }

    @Test
    fun `divine order and primal order unlock their extra cantrip only when chosen`() {
        val thaumaturge = OriginChoices.forClass("cleric", mapOf("divine_order" to listOf("thaumaturge")))
        val protector = OriginChoices.forClass("cleric", mapOf("divine_order" to listOf("protector")))
        assertTrue(thaumaturge.any { it.kind == ChoiceKind.SPELL })
        assertFalse(protector.any { it.kind == ChoiceKind.SPELL })

        val magician = OriginChoices.forClass("druid", mapOf("primal_order" to listOf("magician")))
        val warden = OriginChoices.forClass("druid", mapOf("primal_order" to listOf("warden")))
        assertTrue(magician.any { it.kind == ChoiceKind.SPELL })
        assertFalse(warden.any { it.kind == ChoiceKind.SPELL })
    }

    @Test
    fun `every origin choice on every class and background pairing is answerable`() {
        ClassData.ALL.forEach { charClass ->
            BackgroundData.ALL.forEach { background ->
                choicesFor(background.id, classId = charClass.id).forEach { choice ->
                    assertTrue(
                        "${charClass.id}/${background.id}: '${choice.id}' has no options",
                        choice.options.isNotEmpty(),
                    )
                    assertTrue(
                        "${charClass.id}/${background.id}: '${choice.id}' wants ${choice.count} " +
                            "of ${choice.options.size}",
                        choice.count <= choice.options.size,
                    )
                }
            }
        }
    }

    @Test
    fun `every background feat exists in the feat catalog`() {
        BackgroundData.ALL.forEach { background ->
            assertNotNull(
                "${background.name} references unknown feat '${background.featId}'",
                FeatData.byId(background.featId),
            )
        }
    }
}
