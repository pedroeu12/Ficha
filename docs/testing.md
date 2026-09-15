# The filter

This document exists because of one sentence, repeated in different words over several months:

> *the same bugs I already asked you to fix keep coming back somewhere else.*

That is not a complaint about any particular bug. It is a complaint about a process where a
bug is found by a person building a character, fixed in the one place they found it, and left
alive everywhere else — so the next character with an untried subclass finds thirty of them
again, most already reported. Fixing them faster does not help. The only thing that helps is a
check that finds every other instance of a bug at the moment the first one is reported.

That check is four layers, all running on the JVM in about half a minute, all in
`app/src/test/java/com/pedroeu/ficha/`.

## The protocol

**When a bug is reported, write the rule down first, then fix what it lists.**

Not "fix it and then add a test for it" — the rule comes first, because the rule's failure
message *is the list of everywhere else the bug lives*. The Eldritch Invocation editor was the
case that proved it: one reported bug, and writing it down as a rule turned up the same defect
in three more editors, a second layout, and every list in the game longer than a dialog.

## Layer 1 — invariants: `SheetAudit`

About twenty things that must be true of any character sheet, written as *complaints* rather
than assertions so that one sweep reports everything wrong at once instead of the first thing:

- every question the sheet asks can be answered (it has options, and not fewer than it asks for)
- what the sheet shows and what the rules resolve agree
- every stored answer is still one of the options
- no duplicate choice ids, spells, or pools; no unresolved grants
- prepared spells within the limit; current hit points within the maximum; spent within max
- a long rest restores what a long rest restores

`SheetAudit.fullyAnswered(character)` answers every open question to a fixed point, so the
invariants can be checked on a *finished* character rather than a blank one.

## Layer 2 — every piece of content: `EveryContentSweepTest`

Runs `SheetAudit` over the whole game, not over a sample:

| Sweep | Coverage |
| --- | --- |
| every subclass | at its first and last feature level, blank and fully answered |
| every class | at levels 1, 5, 11 and 20 |
| every species × lineage | on a Wizard and a Fighter |
| every background | plain and answered |
| every feat | on a Fighter and a Wizard |
| every pair of classes | as a 5/4 multiclass |

This is the layer that answers "what happens when I make a character I have never made
before", and it answers it for all of them, in about four seconds.

## Layer 3 — the shape of a mistake in the source: `UiInvariantTest`

Some bugs are not findable from the rules or from a character, because they are in the wiring.
Each rule here is a bug that shipped, was fixed where it was reported, and came back elsewhere:

1. a dialog that lists options can be scrolled to the end of them
2. no screen remembers a copy of something the player is editing (remember the id, resolve it live)
3. every picker greys out what the character cannot take (`disabledOptionIds`)
4. a dialog never offers to reset a number to the value already pinned on it
5. the phone and the tablet do not keep their own copies of the same editor
6. every picker lets the player write their own option (`onWriteOwn`)

Adding a rule here is the cheapest thing in the repository.

## Layer 4 — the screen, drawn: `ScreenInvariantTest` and `SheetScreenTest`

Reading the source catches the *shape* of a mistake; drawing the screen catches the mistake.
"Every option in this list can be reached" is a fact about the layout, not about the code —
twenty-eight invocations poured into a dialog that clips at the fold reads perfectly well as
source. Robolectric renders real Compose on the JVM, so a test can scroll and tap.

- **`ScreenInvariantTest`** renders the real `ChoiceEditDialog` and, for *every list in the
  game with twelve options or more*, scrolls to the last option, checks the dialog's own
  button survived, and taps the last option the character is allowed to take. Removing the
  scroll from the dialog fails all four of its tests, which is the check that the check works.
- **`SheetScreenTest`** opens the real `CharacterSheetScreen` — real repository, real view
  model — for seven characters chosen for their shapes (Warlock, Wizard, Cleric, Rogue, Druid,
  Battle Master, and a Fighter/Wizard), and walks all seven phone tabs and all four tablet
  leaves, with Edit Mode off and on. Twenty-eight combinations, each asserting the page is
  the character's own and has something on it.

### What the JVM cannot tell you

There are no fonts, so every string is laid out at invented widths. Anything that depends on
real glyph metrics — whether a label truncates, whether two things collide — is not decidable
here, which is why page navigation in `SheetScreenTest` uses a tab's own click action rather
than a tap aimed at a rectangle that exists on no phone. Where geometry *is* the point, as in
a list too long for its dialog, `ScreenInvariantTest` scrolls and taps for real.

## Running it

```
./gradlew testDebugUnitTest            # all of it
./gradlew testDebugUnitTest --tests "com.pedroeu.ficha.ScreenInvariantTest"
```

`ScreenSmokeTest` exists so that when the rendering harness itself breaks — which looks like a
hundred unrelated failures — one test says plainly whether a screen can be drawn at all.
