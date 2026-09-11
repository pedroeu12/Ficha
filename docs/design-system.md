# The Ficha design system

One page, because a design system nobody can hold in their head is a style guide.

## Where it lives

`ui/design/Tokens.kt` — the measurements.
`ui/components/` — the vocabulary built from them.

Nothing outside `ui/design` names a number for spacing, rounding or duration. This is
enforced by `DesignConsistencyTest`, which reads the sources: a screen that invents its own
fails there rather than at the table.

## Space

A 4dp grid, named for the job rather than the size.

| Token | Use |
| --- | --- |
| `Space.tight` | Between a label and the value it labels |
| `Space.inline` | Inside a chip, between an icon and its text |
| `Space.betweenRows` | Between list rows, and under a heading |
| `Space.cardPadding` | From a card's edge to its content |
| `Space.betweenCards` | Between cards in a column |
| `Space.screenEdge` | From the screen edge to the content |
| `Space.sheetEdge` | Inside a bottom sheet |

## Corners

Four radii, each with a job: `Corner.bar` for a progress bar's ends, `Corner.small` for chips
and marks, `Corner.row` for list rows and anything nested, `Corner.card` for the outermost
surface. `Corner.page` is the tablet's sheet of paper.

## Motion

Three durations and one curve. `Motion.quick` (120ms) for a colour or alpha change;
`Motion.standard` (220ms) for anything changing a surface's size or contents;
`Motion.emphasis` (320ms) for a whole screen or sheet arriving. The easing is the same
decelerating curve throughout, which is what makes unrelated animations read as one interface.

## Reading anything

**One gesture, one outcome.** Tapping something with a description opens `DetailSheet`,
wherever you are and however long the text is. There is no second behaviour for short text,
no inline unfolding, and no per-screen sheet.

Every description has the same anatomy, in this order:

1. **Title** — what it is called
2. **Kind** — "Level 3 Evocation", "Martial Weapon", "Eldritch Invocation"
3. **Summary** — the one line you need mid-turn
4. **Facts** — the numbers, as labelled pairs
5. **Body** — the book's own words, in full
6. **Footnote** — where it came from, or what it asks of you

Fields are optional; the order never changes. Actions on the subject — renaming, preparing —
go below the text, so reading is never interrupted by a row of buttons.

A **task surface** is different and is allowed to be: a picker, the rest flow, the item
builder. Those are places to do something, not places to read something.

## Lists

`DetailRow` is every list row in the app. It carries the three states a player has to read at
a glance:

- **Openable** — a chevron on the right. No chevron means tapping does nothing.
- **Selected** — an accented border and tint, animated, not a tick buried in the text.
- **Unavailable** — dimmed as one piece, with the reason written out.

`SectionDisclosure` is every foldable heading: one chevron that rotates, one duration.

## Picking

`SelectableCard` for a choice with rules text behind it, `ChoiceChip` for a short one. Long
text is clamped with "Read the rules", which opens the same sheet as everything else.
