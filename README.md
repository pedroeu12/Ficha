# Ficha

An Android character sheet app for **D&D 2024 (5.5e)**, built with Kotlin and Jetpack Compose.

Create a character by walking a guided wizard — species, class (with all the level‑1 choices that class
implies), origin, ability scores, and finally a name — then manage them on a live character sheet laid
out after the official 2024 sheet.

## The creation flow

The wizard runs in the order you'd fill out the paper sheet, and the **Next** button stays disabled
until every decision on the current step is made.

1. **Species** — 10 species. Selecting one expands its traits inline and surfaces any sub‑choice it
   carries: Elven Lineage, Draconic Ancestry, Giant Ancestry, Fiendish Legacy, Gnomish Lineage, and
   the Human bonus skill.
2. **Class** — 12 classes, each expanding to show its level‑1 features, saving throws, and armor,
   weapon, and tool proficiencies.
3. **Class Options** — the in‑class decisions, generated from the class you picked:
   - skill proficiencies (skills already granted by your species or origin are shown as unavailable,
     so you can't waste a pick),
   - feature options — Fighting Style, Divine Order, Primal Order,
   - cantrips and level‑1 spells for casters,
   - Expertise for the Rogue, drawn from the skills you're actually proficient in.
4. **Origin** — 16 backgrounds, each granting two skills, a tool, an origin feat, equipment, and
   starting gold. You choose the `+2 / +1` or `+1 / +1 / +1` ability spread and tap to assign it.
5. **Ability Scores** — four methods: Standard Array, Point Buy (27‑point budget, live counter),
   4d6‑drop‑lowest with reroll, or manual entry. A running preview shows final scores with origin
   bonuses folded in.
6. **Name & Details** — name, alignment, appearance, backstory, plus a summary card of the whole build.

## The character sheet

Seven tabs, modeled on the official 2024 sheet layout:

| Tab | Contents |
| --- | --- |
| **Stats** | AC, initiative, speed, size, passive Perception, proficiency bonus, Heroic Inspiration, hit points with quick ±1/±5 adjusters, temp HP, hit dice, death saves |
| **Skills** | All 18 skills grouped under their governing ability, with proficiency and expertise pips |
| **Combat** | Weapon attack table (attack bonus, damage, properties), equipment training, defenses |
| **Features** | Class features, chosen feature options, species traits, lineage, feats |
| **Spells** | Spellcasting ability/save DC/attack bonus, tappable spell slots, cantrips and known spells |
| **Inventory** | Equippable gear (toggling armor recalculates AC live), free‑form items, coins in all five denominations |
| **Bio** | Identity summary, appearance, backstory, session notes |

Everything is calculated, not typed: AC responds to what you equip, skill bonuses to proficiency and
expertise, HP to your class hit die and Constitution. The toolbar's rest button restores HP, clears
death saves, and refreshes spell slots.

## Building

Standard Android Gradle project — open in Android Studio and run, or from the command line:

```bash
./gradlew assembleDebug      # APK at app/build/outputs/apk/debug/
./gradlew installDebug       # build and install onto a connected device
./gradlew test               # unit tests for the rules engine
```

Requirements: JDK 17+, Android SDK with API 35 (`compileSdk 35`, `minSdk 26`).

Characters are stored locally in a Room database; nothing is sent anywhere.

## Project layout

```
app/src/main/java/com/pedroeu/ficha/
├── data/
│   ├── model/      Ability, Skill, Species, CharClass, Background, Feat, Equipment
│   ├── content/    The rules content: species, classes, backgrounds, feats, equipment tables
│   └── db/         Room entity, DAO, database
├── domain/         PlayerCharacter + the calculation engine (AC, HP, saves, skills, attacks)
└── ui/
    ├── creation/   The six-step wizard and its state machine
    ├── sheet/      The seven sheet tabs
    ├── home/       Character list
    └── theme/      Colors, typography, Material 3 theme
```

The rules content is plain Kotlin data in `data/content/` — adding a species, class, background, or
weapon means adding an entry to a list, and the wizard and sheet pick it up automatically.

## Tests

39 unit tests cover the parts worth getting right: ability modifiers, proficiency scaling, AC across
armor categories and both Unarmored Defense variants, hit points including Dwarven Toughness and the
Tough feat, skill and saving throw bonuses, finesse weapon attacks, spell save DCs, point-buy costs,
and every gating rule in the creation wizard.

```bash
./gradlew test
```

## Scope

Characters are built at **level 1**. Subclasses, leveling up, and multiclassing aren't implemented —
the sheet has fields for them but no progression logic. The spell lists are a curated subset for
level‑1 casters rather than the complete spell compendium.

Game rules content is from the D&D 2024 rules, © Wizards of the Coast. This project is an unofficial
personal tool and is not affiliated with or endorsed by Wizards of the Coast.
