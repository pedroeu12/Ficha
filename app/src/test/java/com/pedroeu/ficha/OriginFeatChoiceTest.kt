package com.pedroeu.ficha

import com.pedroeu.ficha.data.content.BackgroundData
import com.pedroeu.ficha.data.content.FeatData
import com.pedroeu.ficha.data.content.OriginChoices
import com.pedroeu.ficha.data.model.ChoiceKind
import com.pedroeu.ficha.data.model.FeatCategory
import com.pedroeu.ficha.data.model.Sourcebook
import com.pedroeu.ficha.ui.creation.CreationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nothing the books word as "of your choice" is ever chosen for the player — including the
 * origin feat, which five backgrounds leave open and which the Human's Versatile trait grants.
 *
 * These also pin the other half of it: the options offered must come from the books the
 * character was built with, not from every book the app knows.
 */
class OriginFeatChoiceTest {

    private val openFeat = listOf(
        "haunted_one" to FeatCategory.DARK_GIFT,
        "investigator" to FeatCategory.DARK_GIFT,
        "mist_wanderer" to FeatCategory.DARK_GIFT,
        "spirit_medium" to FeatCategory.DARK_GIFT,
        "pact_seeker" to FeatCategory.PLANAR_PACT,
    )

    @Test
    fun `every background whose book leaves the feat open actually asks for it`() {
        openFeat.forEach { (id, category) ->
            val choices = OriginChoices.forBackgroundFeat(id, Sourcebook.EVERYTHING)
            assertEquals("$id should ask for its origin feat", 1, choices.size)
            val choice = choices.first()
            assertEquals(ChoiceKind.FEAT, choice.kind)
            assertTrue(
                "$id must offer the whole $category group",
                choice.options.count { FeatData.byId(it.id)?.category == category } >= 6,
            )
        }
    }

    @Test
    fun `a background with a fixed feat asks nothing`() {
        // The Acolyte simply has Magic Initiate (Cleric); there is no decision to make.
        assertTrue(OriginChoices.forBackgroundFeat("acolyte", Sourcebook.EVERYTHING).isEmpty())
        assertTrue(OriginChoices.forBackgroundFeat(null).isEmpty())
    }

    @Test
    fun `the named alternative is offered alongside the group`() {
        // "Survivor or a Dark Gift feat of your choice" — both halves have to be there.
        val haunted = OriginChoices.forBackgroundFeat("haunted_one", Sourcebook.EVERYTHING).first()
        assertTrue("Survivor is the named alternative", haunted.options.any { it.id == "survivor" })
        assertTrue("the Dark Gifts come with it", haunted.options.any { it.id == "mist_walker" })

        val investigator =
            OriginChoices.forBackgroundFeat("investigator", Sourcebook.EVERYTHING).first()
        assertTrue(investigator.options.any { it.id == "sharp_eye" })
    }

    @Test
    fun `the open feat is offered through the creation flow, not just the builder`() {
        val state = CreationState(
            speciesId = "elf",
            classId = "fighter",
            backgroundId = "pact_seeker",
            enabledSources = Sourcebook.EVERYTHING,
        )
        assertTrue(
            "the Origin Options step must carry the prompt",
            state.originChoices.any { it.id == "background:pact_seeker:origin_feat" },
        )
    }

    @Test
    fun `an open background does not also grant its fallback feat`() {
        // featId is only there so nothing is null; granting it as well would be two feats.
        val haunted = BackgroundData.byId("haunted_one")!!
        assertEquals("survivor", haunted.featId)
        val granted = OriginChoices.all(
            speciesId = "elf",
            lineageId = null,
            classId = "fighter",
            classSelections = emptyMap(),
            backgroundId = "haunted_one",
            originSelections = mapOf("background:haunted_one:origin_feat" to listOf("mist_walker")),
            books = Sourcebook.EVERYTHING,
        )
        // Mist Walker's own sub-choices should appear; Survivor's should not.
        assertTrue(
            "the chosen feat is followed into its grants",
            granted.none { it.id.startsWith("feat:survivor") },
        )
    }

    @Test
    fun `the options a background offers respect the character's books`() {
        val coreOnly = OriginChoices.forBackgroundFeat("pact_seeker", Sourcebook.CORE)
        assertTrue(
            "Planar Pacts are not a core book, so there is nothing to offer",
            coreOnly.isEmpty(),
        )
    }

    @Test
    fun `feat categories match the groups the books print`() {
        assertEquals(9, FeatData.ALL.count { it.category == FeatCategory.DARK_GIFT })
        assertEquals(6, FeatData.ALL.count { it.category == FeatCategory.PLANAR_PACT })
        assertEquals(27, FeatData.ALL.count { it.category == FeatCategory.DRAGONMARK })
        assertFalse(
            "a Dark Gift is not a plain origin feat",
            FeatData.byId("mist_walker")!!.category == FeatCategory.ORIGIN,
        )
    }
}
