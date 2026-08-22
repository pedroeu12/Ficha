# Ficha

An Android character sheet app for **D&D 2024 (5.5e)**, built with Kotlin and Jetpack Compose.

Create a character by walking a guided wizard — species, class (with all the level‑1 choices that class
implies), origin, ability scores, and finally a name — then manage them on a live character sheet laid
out after the official 2024 sheet, level them up through 20, and edit anything the rules got wrong for
your table.

## A rule the whole app follows

**Nothing that says "of your choice" is ever chosen for you.** Every grant the rulebook leaves open —
spells, skills, tool proficiencies, ability score increases, subclasses, fighting styles, damage types,
lineage cantrips — is surfaced as an explicit prompt, whether it comes from a species, a class, a
background, an origin feat, or a level‑up. If a step still has an open decision, the **Next** button
stays disabled.

## The creation flow

1. **Species** — 24 species. Selecting one expands its traits inline and surfaces any sub‑choice it
   carries: Elven Lineage, Draconic Ancestry, Giant Ancestry, Fiendish Legacy, Gnomish Lineage, and
   the Human bonus skill.
2. **Class** — 13 classes, each expanding to show its level‑1 features, saving throws, and armor,
   weapon, and tool proficiencies.
3. **Class Options** — the in‑class decisions, generated from the class you picked:
   - skill proficiencies (skills already granted by your species or origin are shown as unavailable,
     so you can't waste a pick),
   - feature options — Fighting Style, Divine Order, Primal Order,
   - cantrips and level‑1 spells for casters,
   - Expertise for the Rogue, drawn from the skills you're actually proficient in.
4. **Origin** — 61 backgrounds, each granting two skills, a tool, an origin feat, equipment, and
   starting gold. You choose the `+2 / +1` or `+1 / +1 / +1` ability spread and tap to assign it.
5. **Origin Options** — everything the previous steps left open, in one place:
   - **Magic Initiate spells** — a Sage picks two Wizard cantrips and a level 1 spell, an Acolyte
     picks from the Cleric list, a Guide from the Druid list,
   - **Skilled / Crafter / Musician** feat picks,
   - **"Artisan's Tools of your choice"** and friends, resolved to a specific tool,
   - a **High Elf's** Wizard cantrip, a **Bard's** three instruments, a **Monk's** tool,
   - the extra cantrip a **Thaumaturge** Cleric or **Magician** Druid gets.
6. **Ability Scores** — four methods: Standard Array, Point Buy (27‑point budget, live counter),
   4d6‑drop‑lowest with reroll, or manual entry. A running preview shows final scores with origin
   bonuses folded in.
7. **Name & Details** — name, alignment, appearance, backstory, plus a summary card of the whole build.

## Levelling up

The **Level Up** button in the sheet toolbar runs the same guided treatment, scoped to the next level
and driven by real class tables for all 13 classes across levels 1–20:

- **Hit points** — take the fixed average, roll the die in‑app, or type your table's house rule.
- **Subclass** — every class chooses at level 3, from the full 2024 list plus the playtest options.
  Each entry expands to show its whole feature progression before you commit.
- **New features** — everything the class and subclass tables grant at that level, with any decision
  they carry (Battle Master maneuvers, Metamagic, Eldritch Invocations, Arcane Shot options,
  Wild Heart aspects, Transmuter's Stone, Circle of the Land terrain, and so on).
- **Ability Score Improvement** — `+2` to one ability, `+1` to two, or a feat from the general list.
  Level 19 offers Epic Boons instead. Fighters get theirs at 6 and 14, Rogues at 10.
- **Spells** — new cantrips and prepared/known spells drawn from your class list, sized by the class
  table. Above the catalogued range a free‑text box keeps high‑level casters unblocked.
- **Review** — a summary of every change before it's written.

### Subclasses

All 2024 Player's Handbook subclasses (4 per class, 48 total), plus the six playtest subclasses from
**Unearthed Arcana 2025: Arcane Updates** — Arcane Archer (Fighter), Tattooed Warrior (Monk), and
Conjurer, Enchanter, Necromancer, and Transmuter (Wizard). Playtest entries are labelled as such
wherever they appear, so they're never mistaken for published material.

## The character sheet

Seven tabs, modeled on the official 2024 sheet layout:

| Tab | Contents |
| --- | --- |
| **Stats** | AC, initiative, speed, size, passive Perception, proficiency bonus, Heroic Inspiration, hit points with quick ±1/±5 adjusters, temp HP, hit dice, death saves, and every limited-use tracker |
| **Skills** | All 18 skills grouped under their governing ability, with proficiency and expertise pips, plus tool proficiencies |
| **Combat** | Weapon attack table (attack bonus, damage, properties), your own custom attacks, equipment training, defenses |
| **Features** | Class features by level, subclass features, species traits, lineage, feats, and **the actual choices made inside each feature** |
| **Spells** | Spellcasting ability/save DC/attack bonus, prepared and cantrip capacity, tappable spell slots, known spells with preparation toggles |
| **Inventory** | Equippable gear (toggling armor recalculates AC live), coins in all five denominations, and a tap‑through detail view for every item |
| **Bio** | Identity summary, appearance, backstory, session notes |

Everything is calculated, not typed: AC responds to what you equip, skill bonuses to proficiency and
expertise, HP to your class hit die and Constitution — until you decide otherwise in Edit Mode.

### Inventory

Tapping any item opens its full rulebook entry — description, cost, weight, and the stats that matter
(damage and mastery property for weapons, AC and Strength requirement for armor). **Add an item** offers
two routes: browse or search the rulebook catalog of ~124 weapons, armor, packs, tools, and gear, which
fills in every stat automatically; or write a custom entry for anything homebrewed. Items added before
the catalog existed still resolve by name, so older characters get descriptions too.

### Limited-use trackers

Anything with a set number of uses gets a counter rather than a sentence buried in a description:
Monk Focus Points, Sorcery Points, Rage, Second Wind, Action Surge, Channel Divinity, Wild Shape,
Lay on Hands, Bardic Inspiration, Superiority and Psionic dice, Portent, Arcane Ward — and the small
ones that are easiest to forget, like a Dragonborn's Breath Weapon, an Orc's Relentless Endurance, a
Lucky feat's Luck Points, or the single free casting Magic Initiate grants once per Long Rest.

Trackers know how they recharge, so rests refill exactly the right ones. Small pools show tappable
pips; large ones (Focus, Sorcery Points, Lay on Hands) show a counter. Every maximum is editable, and
**Track something else** adds your own pool for anything the app doesn't model.

### Short Rest and Long Rest

**Short Rest** opens a panel to spend Hit Dice one at a time — roll in-app or take the average — showing
what each die restores before you commit. It then returns every short-rest resource, plus a Warlock's
Pact Magic slots. **Long Rest** restores hit points, half your Hit Dice, all spell slots, every
resource, and clears death saves. Both report exactly what they put back rather than silently changing
numbers, and both offer any pick the rules let you revisit while resting — a Tattooed Warrior reshaping
a tattoo, a Fiend Warlock changing their resistance, a Transmuter re-attuning their stone.

### Edit Mode

The pencil in the toolbar turns the sheet fully editable, for the times the rules engine and your DM
disagree. **Nothing is read-only while it's on** — every number and every piece of text, including
your character's name, feature names and descriptions, species trait text, proficiency lines, item
names and notes, and identity fields. Rules text is never destroyed: your version is stored as an
override, and every edited field can be reset back to the rulebook.

For numbers, two mechanisms compose and are kept distinct:

- a **bonus** adds to whatever the rules produce — the right tool for "+2 Stealth for this arc",
- an **override** replaces the value outright, for anything the app doesn't model.

Overrides always win, adjusted values are tinted so you can see at a glance what's been touched, and
**Reset all** returns the sheet to the rules. In Edit Mode you can:

- toggle any skill through none → proficient → expertise, and add a bonus or override to its total,
- grant or revoke saving throw proficiency independently of your class,
- edit ability scores, AC, initiative, speed, max HP, proficiency bonus, passive Perception,
  spell save DC, spell attack bonus, and prepared/cantrip capacity,
- set spell slots per level 1–9 outright, or reset them to the class table,
- add and remove tool proficiencies, spells, feats, features, attacks, and inventory items,
- change any limited-use maximum, or hide a tracker entirely by setting it to zero,
- re-pick any choice a feature granted, at any time.

### Adding things

Every category has an **Add** button that opens the matching rulebook list:

| Category | What opens |
| --- | --- |
| **Feats** | The full feat list, searchable. A feat carrying its own decisions — Magic Initiate's spells, Skilled's proficiencies, Crafter's tools — asks for them before it can be added |
| **Spells** | The spell catalog, filterable by level and by your class list, plus add-by-name for anything beyond it |
| **Equipment** | 124 weapons, armor, packs, tools, and gear, with stats filled in automatically |
| **Attacks** | A form for name, damage die, attack/damage bonus, damage type, range, and free-text special effects |
| **Features** | A free-text feature for anything homebrewed |
| **Trackers** | A custom limited-use pool, with its own recharge rule |

## Building

Standard Android Gradle project — open in Android Studio and run, or from the command line:

```bash
./gradlew assembleDebug      # APK at app/build/outputs/apk/debug/
./gradlew installDebug       # build and install onto a connected device
./gradlew test               # unit tests for the rules engine
```

Requirements: JDK 17+, Android SDK with API 35 (`compileSdk 35`, `minSdk 26`).

Characters are stored locally in a Room database; nothing is sent anywhere. The stored format tolerates
older saves — characters created before levelling existed load fine and keep their data.

## Project layout

```
app/src/main/java/com/pedroeu/ficha/
├── data/
│   ├── model/      Ability, Skill, Species, CharClass, Background, Feat, Equipment,
│   │               Choice, SpellDef, ClassProgression, Subclass
│   ├── content/    The rules content: species, classes, subclasses, backgrounds, feats,
│   │               spells, equipment, the item catalog, and the level tables
│   └── db/         Room entity, DAO, database
├── domain/         PlayerCharacter + the calculation engine (AC, HP, saves, skills, attacks,
│                   spell slots, the override layer, limited-use resources, the rest engine,
│                   and the choice resolver)
└── ui/
    ├── creation/   The seven-step wizard and its state machine
    ├── levelup/    The level-up flow and its state machine
    ├── sheet/      The seven sheet tabs, item detail, and the rulebook picker
    ├── home/       Character list
    ├── components/ Shared choice, card, and edit controls
    └── theme/      Parchment palette, typography, Material 3 theme
```

The rules content is plain Kotlin data in `data/content/` — adding a species, class, subclass,
background, spell, or item means adding an entry to a list, and the wizard, level-up flow, and sheet
pick it up automatically.

## Look

A single warm parchment theme, deliberately with no dark variant: an aged sheet that flipped to grey
in dark mode would defeat the point. Cream and beige paper tones under a soft vertical wash, dark brown
ink for text, crimson chrome, and gold section headings.

## Tests

110 unit tests over the parts worth getting right:

```bash
./gradlew test
```

- ability modifiers, proficiency scaling, AC across armor categories and every Unarmored Defense
  variant, hit points including recorded level-up rolls, Dwarven Toughness, and the Tough feat,
- skill and saving throw bonuses, finesse weapon attacks, spell save DCs, point-buy costs,
- every gating rule in the creation wizard,
- the Edit Mode override layer: bonuses stack, overrides replace, overrides beat bonuses,
- the full progression tables — spell slots for all three caster types, ASI levels per class,
  subclass timing, and that every level-up choice for every class is answerable,
- that every "of your choice" grant is actually presented, including the Sage's Magic Initiate spells,
- limited-use derivation for every class, subclass, species, and feat at every level, and that no
  tracker is ever generated empty or nameless,
- Short Rest hit-dice spending and Long Rest restoration, including which resources each one refills
  and that a Warlock's Pact slots come back on a Short Rest while a Wizard's do not,
- that a choice made inside a feature resolves back to readable names — the Monk's tattoos, Battle
  Master maneuvers, Metamagic, Fighting Styles taken after level 1, and Magic Initiate spells.

## Scope

The spell catalog covers all 419 spells, cantrips through level 9, and anything outside it can still be
added by name wherever spells are chosen. Subclass features are recorded with their names, levels, and
descriptions; their numeric effects aren't wired into the calculation engine, so a feature that changes
a derived number is applied through Edit Mode.

Game rules content is from the D&D 2024 books, © Wizards of the Coast. Every option names the book it
came from, and a character is built from a chosen set of books — creation, level up and the sheet's
pickers only ever offer what those books contain. The Unearthed Arcana options are playtest material and
are not official game content. This project is an unofficial personal tool and
is not affiliated with or endorsed by Wizards of the Coast.
