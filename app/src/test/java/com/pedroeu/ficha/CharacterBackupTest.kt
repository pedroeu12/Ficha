package com.pedroeu.ficha

import com.pedroeu.ficha.data.CharacterBackup
import com.pedroeu.ficha.data.model.Ability
import com.pedroeu.ficha.domain.KnownSpell
import com.pedroeu.ficha.domain.PlayerCharacter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Carrying characters between installs.
 *
 * Updating the app means installing over the old build, and when that doesn't take, the
 * advice is to uninstall first — which deletes the database. The file has to survive that,
 * and it has to survive the app changing shape underneath it, which is the part worth
 * testing: a backup written by an older build has to open in a newer one and the reverse.
 */
class CharacterBackupTest {

    private fun character(
        id: String = "a",
        name: String = "Varek",
        level: Int = 5,
        updatedAt: Long = 1_000,
    ) = PlayerCharacter(
        id = id,
        name = name,
        speciesId = "human",
        classId = "fighter",
        backgroundId = "soldier",
        level = level,
        updatedAt = updatedAt,
        baseAbilityScores = Ability.ALL.associate { it.name to 14 },
        knownSpells = listOf(
            KnownSpell("fire_bolt", "Fire Bolt", 0, "Evocation", "A mote of fire.")
        ),
        featIds = listOf("alert"),
    )

    @Test
    fun `a character survives the round trip unchanged`() {
        val original = character()
        val text = CharacterBackup.encode(listOf(original), "2.4 (16)", now = 0)
        val restored = CharacterBackup.decode(text).getOrThrow()

        assertEquals(1, restored.size)
        assertEquals(
            "everything about the character must come back, not just its name",
            original,
            restored.first(),
        )
    }

    @Test
    fun `several characters come back in one file`() {
        val all = listOf(character("a"), character("b", "Sszaria"), character("c", "Ilda"))
        val restored = CharacterBackup
            .decode(CharacterBackup.encode(all, "2.4 (16)", now = 0))
            .getOrThrow()

        assertEquals(all, restored)
    }

    @Test
    fun `the file records which build wrote it`() {
        val text = CharacterBackup.encode(listOf(character()), "2.4 (16)", now = 1_700_000_000_000)
        assertTrue("the app version is worth knowing when a file won't open", text.contains("2.4 (16)"))
        assertTrue(text.contains("\"format\""))
        assertTrue(text.contains("1700000000000"))
    }

    // ------------------------------------------------------------- Across versions

    @Test
    fun `a backup from an older build opens in a newer one`() {
        // A file written before several fields existed: no classLevels, no levelSelections,
        // no weapon masteries. Those all have defaults, so the character still opens.
        val old = """
            {
              "format": 1,
              "appVersion": "1.0 (1)",
              "exportedAt": 0,
              "characters": [
                {
                  "id": "old",
                  "name": "Varek",
                  "speciesId": "human",
                  "classId": "fighter",
                  "backgroundId": "soldier",
                  "level": 3
                }
              ]
            }
        """.trimIndent()

        val restored = CharacterBackup.decode(old).getOrThrow()
        assertEquals(1, restored.size)
        assertEquals("Varek", restored.first().name)
        assertEquals(3, restored.first().level)
        assertTrue("fields added later fall back", restored.first().classLevels.isEmpty())
    }

    @Test
    fun `a backup carrying fields this build doesn't know is still read`() {
        // The reverse case: a file from a newer build, with a field added after this one.
        // Ignoring it beats refusing the whole file, which would strand the character.
        val newer = """
            {
              "format": 1,
              "appVersion": "9.9 (99)",
              "exportedAt": 0,
              "characters": [
                {
                  "id": "future",
                  "name": "Ilda",
                  "speciesId": "elf",
                  "classId": "wizard",
                  "backgroundId": "sage",
                  "level": 7,
                  "somethingAddedLater": {"a": 1, "b": [2, 3]}
                }
              ]
            }
        """.trimIndent()

        val restored = CharacterBackup.decode(newer).getOrThrow()
        assertEquals("Ilda", restored.first().name)
        assertEquals(7, restored.first().level)
    }

    @Test
    fun `a file in a format this build cannot understand says so`() {
        val future = """{"format": 99, "appVersion": "", "exportedAt": 0, "characters": []}"""
        val failure = CharacterBackup.decode(future)

        assertTrue(failure.isFailure)
        assertTrue(
            "the message should tell the player what to do",
            failure.exceptionOrNull()!!.message!!.contains("newer version"),
        )
    }

    @Test
    fun `a bare list of characters is accepted too`() {
        // What hand-editing a backup, or pasting one character out of it, tends to produce.
        val bare = """
            [
              {
                "id": "bare",
                "name": "Sszaria",
                "speciesId": "human",
                "classId": "wizard",
                "backgroundId": "sage",
                "level": 2
              }
            ]
        """.trimIndent()

        assertEquals("Sszaria", CharacterBackup.decode(bare).getOrThrow().first().name)
    }

    @Test
    fun `a file that isn't a backup fails with a readable reason`() {
        listOf("", "   ", "not json at all", "{\"unrelated\": true, \"x\": 1}")
            .forEach { text ->
                val failure = CharacterBackup.decode(text)
                assertTrue("'$text' should not parse as a backup", failure.isFailure)
                assertNotNull(failure.exceptionOrNull()?.message)
            }
    }

    // ------------------------------------------------------------- Restoring

    @Test
    fun `restoring onto an empty device brings everything back`() {
        val backup = listOf(character("a"), character("b", "Ilda"))
        val plan = CharacterBackup.plan(existing = emptyList(), incoming = backup)

        assertEquals(2, plan.added.size)
        assertTrue(plan.updated.isEmpty())
        assertTrue(plan.skipped.isEmpty())
        assertEquals(backup, plan.toWrite)
    }

    @Test
    fun `restoring the same file twice changes nothing the second time`() {
        val backup = listOf(character("a"))
        val afterFirst = CharacterBackup.plan(emptyList(), backup).toWrite
        val second = CharacterBackup.plan(existing = afterFirst, incoming = backup)

        assertTrue("nothing new", second.added.isEmpty())
        assertTrue("nothing newer", second.updated.isEmpty())
        assertEquals(1, second.skipped.size)
        assertTrue(second.toWrite.isEmpty())
    }

    @Test
    fun `a stale backup never undoes an evening's play`() {
        val played = character("a", level = 9, updatedAt = 5_000)
        val stale = character("a", level = 5, updatedAt = 1_000)

        val plan = CharacterBackup.plan(existing = listOf(played), incoming = listOf(stale))

        assertTrue("the older copy must not be written", plan.toWrite.isEmpty())
        assertEquals(1, plan.skipped.size)
        assertTrue(plan.summary().contains("already here"))
    }

    @Test
    fun `a newer backup does replace what's on the device`() {
        val onDevice = character("a", level = 5, updatedAt = 1_000)
        val newer = character("a", level = 9, updatedAt = 5_000)

        val plan = CharacterBackup.plan(existing = listOf(onDevice), incoming = listOf(newer))

        assertEquals(1, plan.updated.size)
        assertEquals(9, plan.toWrite.first().level)
    }

    @Test
    fun `restoring says what it did`() {
        val plan = CharacterBackup.plan(
            existing = listOf(character("a", updatedAt = 9_000)),
            incoming = listOf(
                character("a", updatedAt = 1_000),
                character("b", "Ilda"),
            ),
        )

        val summary = plan.summary()
        assertTrue(summary, summary.contains("1 restored"))
        assertTrue(summary, summary.contains("1 already up to date"))
    }

    @Test
    fun `an empty backup is reported rather than passed off as a success`() {
        val plan = CharacterBackup.plan(existing = emptyList(), incoming = emptyList())
        assertTrue(plan.isEmpty)
        assertTrue(plan.summary().contains("no characters"))
    }

    // ------------------------------------------------------------- The filename

    @Test
    fun `the filename carries the date so a folder of backups reads well`() {
        val name = CharacterBackup.fileName(1_785_628_800_000)
        assertEquals("ficha-backup-2026-08-02.json", name)

        // The epoch itself, and a leap day, to check the date maths rather than one sample.
        assertEquals("ficha-backup-1970-01-01.json", CharacterBackup.fileName(0))
        assertEquals("ficha-backup-2024-02-29.json", CharacterBackup.fileName(1_709_164_800_000))
    }
}
