# Every choice the rules give the player gets asked

One rule, and it is not negotiable:

> **If an entry's text says the player picks something the sheet records, the app asks for
> it — at the moment the entry is gained, before the flow that granted it can be finished.**

This has been the source of the same bug six times over: a Sage's Magic Initiate granting
cantrips nobody picked, an Origin feat taken through Versatile asking nothing further, always-
prepared spells offered as though they were choices, an Eldritch Invocation added with no way
to name the cantrip it modifies. Every one was fixed for the single feature it was reported on,
because nothing said which features worded a choice and nothing noticed a new one arriving
unasked.

## Where a question lives

A question belongs **on the thing that raises it**, never in a branch somewhere else that
knows that thing by name.

| The thing | Where its question goes |
|---|---|
| A class feature | `ClassFeature.choices` |
| A subclass feature | `SubclassFeature.choices` |
| A species trait | `Trait.choices` |
| A lineage | `LineageOption.choices` |
| A feat | `FeatChoiceData.choicesFor` |
| **An option inside a choice** | **`ChoiceOption.grants`** |

That last row is the one that was missing, and it is why the bug kept returning. An Eldritch
Invocation is an *option* inside the invocation choice; so is a Fighting Style, a Metamagic, a
maneuver, an Artificer plan. Until `grants` existed an option could not carry a question, so
every option that implied one had to be hand-written into a `when (classId)` that named the
Warlock — and a new patron, a new invocation, a new subclass needed someone to remember to
extend it. Nobody did, every time.

## How they get asked

`ChoiceGraph.expand(roots, answers, books)` walks from what has been answered to everything
those answers raise, repeatedly, until nothing new appears. It knows nothing about invocations
or feats or patrons: it looks at the options that were actually picked and asks what they
carry. A choice of kind `FEAT` also pulls in that feat's own questions, so Magic Initiate asks
the same three things whether it came from a background, a species trait, an Ability Score
Improvement, or an invocation.

The walk runs in all four places a choice can be made — character creation, the level-up flow,
the sheet, and Edit Mode — and reads the **live** selections, so a question appears beside the
option as it is ticked. The level-up flow validates against the expanded list, which is what
turns the rule into something the app enforces rather than intends: a level cannot be finished
with a raised question unanswered.

## Choices made at the table

Some text reads like a choice but names no field on a sheet. "Choose an ally within 30 feet"
is a target, decided and forgotten within a turn. "One skill of your choice" is a proficiency
the character carries for life.

The dividing line is the object. If the player picks a **skill, tool, language, feat, spell,
cantrip, damage type, ability score, weapon, maneuver, invocation, instrument, expertise,
proficiency, domain or element**, the sheet holds it and the app must ask. If they pick a
creature, a space, or an effect for this turn only, it belongs in `PerUseChoiceData` — which
asks where the feature is used, and asks again next time — or nowhere.

## The safeguard

`ChoiceCoverageTest` reads every entry in the dataset, finds the ones whose text says the
player picks something recorded, and **fails the build** unless each either raises a real
choice, is covered by `PerUseChoiceData`, or is named in one of two lists with its reason.

So a subclass added next year whose feature says "one skill of your choice" fails the build
until someone writes the question. That is the whole point: the rule is checked by the
build, not remembered by a person.

If the test fails on something you added, do one of these — in this order of preference:

1. **Write the question** on the entry itself, using the table above.
2. **Add a `PerUseChoice`** if the rules have the player decide each time they use it.
3. **Name it in `DECIDED_AT_THE_TABLE` or `ASKED_ELSEWHERE`**, with a sentence saying why.

Reaching for 3 first is how the app got here. Prefer 1.
