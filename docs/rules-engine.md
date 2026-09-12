# A unified rules engine

**Status: Phase 0 done, Phase 1 begun, Phase 3 done.** The schema below is built and in the
repository. What follows describes it; the section at the end records how much of the app
actually runs through it.

## The problem, stated precisely

A single feature's identity is currently spread across up to nine tables, each keyed by a
string id, with nothing checking that they agree:

| What it holds | Where it lives | Keyed by |
|---|---|---|
| Spells granted | `SpellGrantData` | 6 separate maps: class, subclass, species, lineage, feat, choice |
| Limited uses | `ResourceData` | class, subclass, species, feat |
| What a pool pays for | `ResourceOptionData` | resource id |
| Choices made when gained | `ClassFeature.choices`, `SubclassFeature.choices`, `Trait.choices`, `LineageOption.choices`, `ChoiceOption.grants`, `FeatChoiceData`, `OriginChoices` | seven different places |
| Choices made when used | `PerUseChoiceData` | its own id scheme |
| Flat numeric bonuses | `PassiveBonusData` | species, lineage, feat |
| Save DCs | `SaveDcData` | class, subclass, species, feat |
| Attacks | `CharacterAttacks` | derived in code |
| Prerequisites | `ChoiceOption.minLevel`, `requiresOptions`, `Choice.prerequisiteClassId`, `FeatPrerequisiteData` | four places |

The Fiend patron appears in four of these under the same id and nothing joins them. Adding a
subclass means remembering all nine. That is why the same bug keeps coming back under a new
name: **the list of things to remember is nowhere, and it is longer than anyone holds in
their head.**

Three text-scanning tests now catch three of these categories after the fact
(`ChoiceCoverageTest`, `GrantCoverageTest`, `DesignConsistencyTest`). They work, but they are
a smoke alarm, not a fireproof wall.

## The shape of the fix

**One type for every piece of content, carrying every rule it implies.**

```kotlin
data class RuleElement(
    val id: String,               // "subclass:fiend:dark_ones_blessing"
    val name: String,
    val description: String,      // the book's own words, still the source of truth for text
    val source: Source,           // Class | Subclass | Species | Lineage | Background
                                  // | Feat | Option | Item | Spell | Statblock
    val gate: Gate,               // when it applies
    val effects: List<Effect>,    // what it does
    val book: Sourcebook?,
)
```

### `Gate` — prerequisites, with the multiclass distinction made structural

```kotlin
data class Gate(
    val level: Int = 1,
    /** Which level the number counts against. */
    val levelScope: LevelScope = OWNING_CLASS,   // OWNING_CLASS | CHARACTER
    /** Other elements that must already be held. Eldritch Smite needs Pact of the Blade. */
    val requires: List<String> = emptyList(),
    /** Answers that must have been given. The Primordial Patron's element. */
    val whenChosen: List<Answer> = emptyList(),  // choiceId -> optionId
    /** Wording the app cannot check, printed rather than enforced. */
    val statedPrerequisite: String = "",
)
```

`levelScope` is the multiclass fix made into data. Today it is an `if (isClassGrant)` buried
in `CharacterSpells.grantsInEffect`, and the same distinction is re-derived, differently, in
four other places.

### `Effect` — the whole vocabulary

Everything a rule does is one of these. This list is the actual proposal; the rest is
plumbing.

**Automatic grants**

```kotlin
GrantSpell(spellId, mode)     // mode: ALWAYS_PREPARED | KNOWN | ADDED_TO_CLASS_LIST
GrantProficiency(kind, name)  // SKILL | TOOL | LANGUAGE | ARMOR | WEAPON | SAVE
GrantExpertise(skill)
GrantFeat(featId)
GrantAttack(attack)
GrantResistance(damageType)
GrantSense(kind, range)
```

`GrantSpell.mode` is the Dragonmark distinction — "always prepared" versus "added to your
class's spell list" — which today exists only as a comment and cost us an audit to separate.

**Calculated modifiers**

```kotlin
ModifyStat(target, amount: Formula, condition: Condition = Always)
ProvidesSaveDc(label, ability: AbilityRef, note)
```

with

```kotlin
sealed interface Formula {
    Flat(n)
    PerLevel(n, scope)
    AbilityMod(ability: AbilityRef)
    ProficiencyBonus(times = 1)
    Sum(parts) ; Max(parts) ; AtLeast(formula, min)
}

sealed interface AbilityRef {
    Fixed(ability)
    ChosenIn(choiceId)     // "the ability increased by this feat"
}
```

`AbilityRef.ChosenIn` is what makes Infernal Bulwark's AC compute itself. That was a one-off
patch; here it is a field.

**Choices**

```kotlin
AskOnGain(choice)              // decided once, when the element arrives
AskOnUse(choice, poolId)       // decided each time it is used
Swappable(choiceId, on)        // LEVEL_UP | LONG_REST | SHORT_REST
```

and crucially `ChoiceOption` carries `effects: List<Effect>` rather than today's
`grants: List<Choice>`. An option can then grant a spell, modify a stat, summon, or ask
another question — today it can only ask another question, which is a special case of one
row of this table.

**Limited uses**

```kotlin
LimitedUses(poolId, max: Formula, recharge, isPointPool, actionCost)
```

`max` as a `Formula` is what turns "a number of times equal to your Proficiency Bonus" and
"equal to your Charisma modifier (minimum of one)" into data. There are 197 resource
definitions today and their maxima are hand-computed integers per level.

**Summons**

```kotlin
Summons(
    summonId, label,
    pick: SummonPick,
    count: Formula,
    duration, concentration,
    scaling: List<SummonScaling>,   // higher slot -> more, or bigger
)

sealed interface SummonPick {
    Fixed(statblockId)                       // Steel Defender, Vestige Companion
    FromList(statblockIds)                   // Pact of the Chain's eight forms
    Filtered(type, maxCr, extra)             // "a Beast of CR 1/4 or lower"
}
```

`Fixed` and `FromList`/`Filtered` are the two cases the request names, and both are here as
data rather than as two code paths.

A statblock is itself made of `RuleElement`s:

```kotlin
data class Statblock(
    id, name, size, type,
    ac: Formula, hp: Formula, speed,
    abilities: Map<Ability, Int>,
    traits: List<RuleElement>,
    actions: List<StatblockAction>,
    resistances, immunities, senses,
)
```

`ac` and `hp` as `Formula` is what lets the Vestige Companion's "AC 13 plus your Charisma
modifier" and "HP 4 + four times your Warlock level" compute from the summoner, which is the
whole reason a summon needs the engine rather than a static card.

What the character carries:

```kotlin
data class ActiveSummon(
    instanceId, statblockId, sourceId,
    name,                       // renameable: "Wolf 2"
    currentHp, maxHp, tempHp,
    spent: Map<String, Int>,    // its own limited uses
    notes,
)
```

on `PlayerCharacter.activeSummons: List<ActiveSummon>`. The character is stored as a JSON
payload, so this needs no database migration.

### The engine

```kotlin
object RulesEngine {
    fun elementsFor(character): List<RuleElement>
    fun effectsFor(character): List<Applied<Effect>>   // each tagged with its element

    // The typed views every UI layer reads, all derived from effectsFor:
    fun choices(character): List<Choice>
    fun grantedSpells(character): List<GrantedSpell>
    fun pools(character): List<ResourceDef>
    fun statModifiers(character, target): List<Applied<ModifyStat>>
    fun saveDcs(character): List<DcSource>
    fun summonables(character): List<Applied<Summons>>
    fun perUseChoices(character): List<Applied<AskOnUse>>
}
```

`ChoiceGraph`, built this week, becomes the fixed-point pass inside `elementsFor`: an
answered option contributes its effects, and any `AskOnGain` among them becomes a new
question. That is the same walk, generalised from "choices raise choices" to "effects raise
effects".

## Migration

Five phases, each ending green, each its own commit.

**Phase 0 — engine behind adapters.** `RulesEngine` reads the *existing* tables through
adapters and emits `RuleElement`s. No content moves. Proves the schema can express what is
already there before anything is rewritten. A test asserts the adapters emit an element for
every entry in every current table.

**Phase 1 — flip the readers.** `CharacterSpells`, `CharacterResources`,
`CharacterCalculations`, `CharacterDcs`, `PerUseChoices` and `ChoiceResolver` read from
`RulesEngine` instead of the tables. Still no content moved. The 707 existing tests are the
check: none should change.

**Phase 2 — move content, one source at a time**, deleting each adapter as its table empties:
species (24) → lineages (28) → backgrounds (95) → feats (185) → classes (13, 165 features) →
subclasses (87, 461 features) → invocations and masteries → items. Full suite after each.

**Phase 3 — summons.** New content, born in the engine: statblocks, the summon tab, multiple
simultaneous instances, independent HP.

**Phase 4 — the coverage tests become schema checks.** The three text-scanning tests are
replaced by structural ones: an element whose text words a choice must carry `AskOnGain` or
`AskOnUse`; one that promises a spell must carry `GrantSpell`; one that says "uses equal to"
must carry `LimitedUses`. Same guarantee, checked against the schema rather than against
prose.

## What does not fit cleanly

These are flagged now rather than discovered halfway through.

1. **Conditional modifiers.** "While raging", "while unarmoured", "against a creature you can
   see." A closed `Condition` enum can cover the fifteen or so that change a printed number;
   the long tail is open-ended and stays prose. **Decision needed:** enumerate and accept the
   ceiling, or allow an escape hatch.

2. **Spell mechanics.** A spell's effect when cast — damage, areas, saves — is not a
   character-sheet rule. Proposal: spells enter the engine only for what they give the
   character (prepared status, a summon, a DC). The 419 descriptions stay prose.

3. **Magic items.** 418 of them, nearly all prose with per-use effects. Proposal: migrate
   only the ~40 that change a printed number or grant a spell; the rest stay catalogue
   entries. **Decision needed.**

4. **Multiclass spell slots.** The combined-table rule is not a per-element effect. It stays
   in `CharacterCalculations`, fed caster types by the engine.

5. **DM-facing text.** A Ring of Elemental Command's plane, a Feywild gift. These become
   `AskOnGain` with cosmetic options, which is honest but adds no computation.

6. **The 2014/2024 split and playtest material.** Already handled by `Sourcebook`; the
   engine inherits it unchanged.

## Size

Roughly 1,500 content entries across 24 files, plus six domain objects rewritten to read the
engine. The phases above are what keeps that from being one unreviewable change.


---

# Where the migration actually stands

Measured, not estimated. Numbers come from walking thirteen level-20 characters, one per class
with its first subclass, through `RulesEngine.elementsFor` and counting distinct elements and
effects.

## Reachable through the engine: all of it

Every one of the nine tables is read by an adapter and emitted as `RuleElement`s. Across those
thirteen characters the engine produces **375 elements carrying 212 effects**: 68 questions
asked on gain, 55 spell grants, 40 limited-use pools, 15 save DCs, 14 on-use questions, 14
swappable answers, 6 calculated modifiers. A conformance test holds each of those against the
old path for sixteen character shapes, multiclass splits included.

179 of the 375 elements carry no effect at all. Most are correct — a feature whose whole
content is rules text has nothing for the sheet to compute — but some are not, and they are the
honest measure of what is left to describe.

## Load-bearing: 4 of 9 systems

| System | Reads the engine? |
|---|---|
| Passive and conditional modifiers | **yes — the only path** |
| Save DCs (`CharacterDcs`) | **yes** |
| Armor Class, Speed, Initiative, Hit Points (`CharacterCalculations`) | **yes** |
| Granted spells (`CharacterSpells`) | no — reads `SpellGrantData` |
| Limited uses (`CharacterResources`) | no — reads `ResourceData` |
| Choices (`ChoiceResolver`, `ChoiceGraph`) | no — reads the content types directly |
| On-use choices (`PerUseChoices`) | no |
| Attacks (`CharacterAttacks`) | no |
| Summons (`CharacterSummons`) | **yes — born in the engine** |

One reader is gone rather than switched: `PassiveBonuses` was a gatherer walking the bonus
tables, then a thin wrapper around the engine, and is now deleted. Nothing outside `rules/`
gathers a modifier any more, and `CharacterCalculations` moved from *partly* to reading the
engine for every number it derives.

Each switch has paid for itself. The first caught a per-level class bonus being scaled by the
character's level, so a Sorcerer 5 / Fighter 3 gained eight Hit Points from Draconic Resilience
instead of five. The third — the one described below — caught four features whose numbers were
printed on the sheet and applied nowhere, and one that was applied wrongly.

## Declaring effects natively: summons and modifiers

Summoning was built in the schema from the start and has no adapter. Conditional modifiers are
the second: `ModifierData` is written in the vocabulary directly, keyed by the engine's id for
the feature that grants each one, so the level it arrives at, the class it counts against and
the subclass or feat it depends on all come from the feature rather than being restated.

## What conditional modifiers cost, and what they found

This was flagged as the thinnest part of the schema, on the grounds that `ModifyStat` appeared
once across thirteen characters while `CharacterCalculations` held a dozen conditions as code.
Moving them needed four additions, which is the honest price:

- `Effect.SetStatBase` — an *alternative base* rather than a bonus. Unarmored Defense replaces
  what armor gives and only counts when it is higher; written as a modifier it would stack.
- `Condition.All` — two clauses at once, for "while you aren't wearing armor or wielding a
  Shield". Three features say it and each had been read differently.
- `Condition.NotInHeavyArmor` — Fast Movement's caveat.
- `Formula.AtLevels` — a step table, for Unarmored Movement's 10/15/20/25/30.

`Condition.Descriptive`, the escape hatch whose use-count is the signal that the closed list is
too narrow, is still used **zero** times. A test asserts it.

What the migration found, in content that had shipped:

| Feature | Was |
|---|---|
| Defense Fighting Style | +1 AC recorded, displayed, never applied — the most-picked style in the game |
| Gloom Stalker's Dread Ambusher | Wisdom to Initiative, printed mid-paragraph, never applied |
| Ranger's Roving | +10 Speed at level 6, never applied |
| Oath of the Noble Genies' Genie's Splendor | an Unarmored Defense nobody had wired |
| Monk's Unarmored Defense | applied *with* a Shield, withholding the Shield's bonus instead of not applying |

`NumericPassiveCoverageTest` is what makes that a category rather than five fixes: it reads the
text the app ships, finds every sentence promising a standing change to Armor Class, Speed,
Initiative or Hit Point maximum, and fails the build unless each is either applied or listed
with a reason it should not be. Fifteen are listed, every one of them either momentary (a
Bladesong, a Wild Shape form, the turn after a Dash) or somebody else's number (the ally who
drank the elixir). Writing new content that states a number and forgetting to declare it now
fails the build.

## What still does not fit

1. **Momentary conditions** — "while Raging", "while shifted", "while the Bladesong is up" are
   real rules the schema can name and the sheet cannot decide, because it does not track the
   moment. `ConditionEval.isComputable` says so explicitly and such a modifier contributes
   nothing rather than guessing. Tracking those states is the next thing that would make them
   computable, and it is a sheet feature, not a schema one.
2. **Spell mechanics** — 419 spells stay prose. Confirmed by the choice audit: sixteen word a
   choice and all sixteen are made at the table.
3. **Magic items** — 418, of which roughly 40 change a printed number or grant a spell. The
   numeric ones already reach Armor Class through the equipped item's own field; the rest stay
   catalogue entries.
4. **Multiclass spell slots** — stays in `CharacterCalculations`, fed caster types.
5. **DM-facing text** — stays prose.

## What to migrate next

`CharacterSpells` and `CharacterResources`, in that order. Both have a table of their own that
the engine already reads through an adapter, so the switch is a reader change rather than a
content rewrite, and both are systems where a missing grant is exactly the bug class this was
built to end.
