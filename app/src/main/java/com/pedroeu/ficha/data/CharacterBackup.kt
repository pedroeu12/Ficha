package com.pedroeu.ficha.data

import com.pedroeu.ficha.domain.PlayerCharacter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A backup file's contents.
 *
 * [format] is the shape of this wrapper, not the app version — it changes only if the file
 * layout itself has to change, which is what would let a future reader know it is looking at
 * something it doesn't understand. [appVersion] is recorded for diagnosis rather than logic:
 * knowing which build wrote a file is what turns "it won't import" into an answerable question.
 */
@Serializable
data class BackupFile(
    val format: Int = CharacterBackup.FORMAT,
    val appVersion: String = "",
    val exportedAt: Long = 0,
    /**
     * Required, unlike everything above it. Without this the wrapper would happily decode
     * any JSON object at all — every other field has a default — and an unrelated file would
     * be reported as a backup holding nothing rather than as the wrong file.
     */
    val characters: List<PlayerCharacter>,
)

/** What restoring a file would do, worked out before anything is written. */
data class RestorePlan(
    /** Characters not currently on the device, which will be added. */
    val added: List<PlayerCharacter> = emptyList(),
    /** Characters already here whose backup copy is newer, which will be replaced. */
    val updated: List<PlayerCharacter> = emptyList(),
    /** Characters already here at the same age or newer, which are left alone. */
    val skipped: List<PlayerCharacter> = emptyList(),
) {
    val toWrite: List<PlayerCharacter> get() = added + updated
    val isEmpty: Boolean get() = added.isEmpty() && updated.isEmpty() && skipped.isEmpty()

    /** A sentence for the player, since counts alone don't say what happened. */
    fun summary(): String = when {
        isEmpty -> "That file held no characters."
        toWrite.isEmpty() -> "Everything in that backup is already here, and nothing was older."
        else -> buildList {
            if (added.isNotEmpty()) add("${added.size} restored")
            if (updated.isNotEmpty()) add("${updated.size} updated")
            if (skipped.isNotEmpty()) add("${skipped.size} already up to date")
        }.joinToString(", ").replaceFirstChar { it.uppercase() } + "."
    }
}

/**
 * Reading and writing the backup file that carries characters between installs.
 *
 * Updating the app means installing an APK over the old one, and when that fails the advice
 * is to uninstall first — which throws the database away with it. A character that took an
 * evening to build shouldn't be a casualty of a version bump, so this writes them somewhere
 * the uninstall can't reach and reads them back afterwards.
 *
 * Both directions are deliberately forgiving about versions. Unknown fields are ignored, so a
 * file written by a newer build still restores into an older one; missing fields fall back to
 * their defaults, so a file written before a feature existed restores into a build that has
 * it. That is the same tolerance the database itself relies on, which is why a character made
 * in v1.0 still opens today.
 */
object CharacterBackup {

    const val FORMAT: Int = 1

    /** Written into the filename so a folder of backups sorts and reads sensibly. */
    const val FILE_EXTENSION: String = "json"
    const val MIME_TYPE: String = "application/json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** A lenient reader for the characters themselves, used when parsing a bare list. */
    private val lenient = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun encode(
        characters: List<PlayerCharacter>,
        appVersion: String,
        now: Long,
    ): String = json.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            format = FORMAT,
            appVersion = appVersion,
            exportedAt = now,
            characters = characters,
        ),
    )

    /**
     * Reads a backup file.
     *
     * A file whose wrapper doesn't parse is tried again as a bare list of characters, which
     * is what someone hand-editing a backup or pasting one character is most likely to end
     * up with. Failure returns the reason rather than an empty list, so the player is told
     * the file was wrong instead of being shown a silent no-op.
     */
    fun decode(text: String): Result<List<PlayerCharacter>> {
        if (text.isBlank()) return Result.failure(BackupError("That file is empty."))

        runCatching { json.decodeFromString(BackupFile.serializer(), text) }
            .onSuccess { file ->
                if (file.format > FORMAT) {
                    return Result.failure(
                        BackupError(
                            "That backup was written by a newer version of Ficha " +
                                "(format ${file.format}). Update the app and try again.",
                        )
                    )
                }
                return Result.success(file.characters)
            }

        // A bare array of characters, rather than the wrapper.
        runCatching {
            lenient.decodeFromString(kotlinx.serialization.builtins.ListSerializer(
                PlayerCharacter.serializer()
            ), text)
        }.onSuccess { return Result.success(it) }

        return Result.failure(
            BackupError("That file isn't a Ficha backup, or it's damaged.")
        )
    }

    /**
     * What restoring [incoming] over [existing] would do.
     *
     * A character already on the device is replaced only when the backup's copy is newer.
     * That makes restoring safe to do twice, and means a stale backup can't quietly undo an
     * evening's play — the common case after a reinstall is that nothing is here at all, and
     * everything simply comes back.
     */
    fun plan(
        existing: List<PlayerCharacter>,
        incoming: List<PlayerCharacter>,
    ): RestorePlan {
        val byId = existing.associateBy { it.id }
        val added = mutableListOf<PlayerCharacter>()
        val updated = mutableListOf<PlayerCharacter>()
        val skipped = mutableListOf<PlayerCharacter>()

        incoming.forEach { character ->
            val current = byId[character.id]
            when {
                current == null -> added += character
                character.updatedAt > current.updatedAt -> updated += character
                else -> skipped += character
            }
        }

        return RestorePlan(added = added, updated = updated, skipped = skipped)
    }

    /** A default filename, dated so a folder of backups is readable at a glance. */
    fun fileName(now: Long): String {
        val days = now / 86_400_000L
        // A plain date stamp without pulling in a formatter, which keeps this testable off
        // the device: days since the epoch converted to a calendar date.
        val (year, month, day) = civilFromDays(days)
        fun pad(value: Int) = value.toString().padStart(2, '0')
        return "ficha-backup-$year-${pad(month)}-${pad(day)}.$FILE_EXTENSION"
    }

    /** Howard Hinnant's civil-from-days, which is exact and needs no time zone database. */
    private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return Triple((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
    }
}

/** A failure worth showing the player, rather than a stack trace. */
class BackupError(message: String) : Exception(message)
