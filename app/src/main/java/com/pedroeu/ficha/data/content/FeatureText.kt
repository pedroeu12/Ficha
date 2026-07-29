package com.pedroeu.ficha.data.content

/**
 * Every feature's rules text, indexed by its name.
 *
 * Trackers name a pool but not what the pool is for — "Action Surge, 1 use" says nothing
 * about what Action Surge does. The text is already written on the feature itself, so rather
 * than duplicating it this looks it up, which means any feature description that improves
 * improves the tracker too.
 */
object FeatureText {

    private val byName: Map<String, String> by lazy {
        buildMap {
            fun put(name: String, description: String) {
                if (name.isBlank() || description.isBlank()) return
                // First writer wins: class tables are more specific than a species trait
                // that happens to share a name.
                putIfAbsent(normalize(name), description)
            }

            ProgressionData.ALL.forEach { progression ->
                progression.features.forEach { put(it.name, it.description) }
            }
            ClassData.ALL.forEach { charClass ->
                charClass.level1Features.forEach { put(it.name, it.description) }
            }
            SubclassData.ALL.forEach { subclass ->
                subclass.features.forEach { put(it.name, it.description) }
            }
            FeatData.ALL.forEach { put(it.name, it.description) }
            SpeciesData.ALL.forEach { species ->
                species.traits.forEach { put(it.name, it.description) }
                species.lineageOptions.forEach { put(it.name, it.description) }
            }
            // Several pools are free castings of a named spell; the spell's own entry is the
            // rules text a player wants there.
            SpellData.ALL.forEach { put(it.name, it.description) }
        }
    }

    /**
     * Trackers named after the die a feature spends rather than the feature itself. Kept
     * short on purpose — anything else should be fixed by naming the pool after its feature.
     */
    private val ALIASES = mapOf(
        "superiority dice" to "Combat Superiority",
        "portent dice" to "Portent",
    )

    /**
     * The rules text for a feature, or empty when nothing is on file.
     *
     * Tracker names drift from feature names in two predictable ways, so both are tried: a
     * trailing note in parentheses ("Arcane Ward (hit points)") and a "Free" prefix the sheet
     * adds for pools that grant a spell ("Free Hunter's Mark").
     */
    fun forName(name: String): String {
        val trimmed = name.substringBefore('(').trim()
        val alias = ALIASES[normalize(trimmed)]
        return byName[normalize(name)]
            ?: byName[normalize(trimmed)]
            ?: byName[normalize(trimmed.removePrefix("Free "))]
            ?: alias?.let { byName[normalize(it)] }
            ?: ""
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace('’', '\'')
}
