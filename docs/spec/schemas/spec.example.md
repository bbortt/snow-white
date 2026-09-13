<!--
Example spec — copy this shape. Each field below is a bold label; Lens and Status
carry an inline value after the colon, as shown. Status is one of planned |
active | deprecated (planned is the default and generates no traceable). A
"#"/"##" heading is NOT a field. The "## Relations" section links what the spec
realizes, concerns, or relates to — the ids and paths here are placeholders to
replace (a sibling spec is linked by bare filename). Cite each spec by its id
alone; any note goes after the link, not inside it. This file sits beside the
schemas, not in the specs directory, so clew never treats it as a real spec.

A leading "#" heading restating the Title field's text, followed by a
markdownlint-disable comment for MD036, is part of the shape too — see below.
Markdownlint requires a file's first line to be a heading (MD041). It also
flags a bold line that reads like a heading (MD036). Clew's field grammar
requires the opposite for every bold-labelled field below. The heading
satisfies MD041 and gives the document a real, navigable top level without
touching a single field. MD036 has no such escape: every field below it
(Realizes, Related, and Status itself) must stay a bold label, so it stays
suppressed for the file.

Prose in every field — Description, Rationale, Verification Description —
follows the `requirements` skill's sentence-length guidance: split a sentence
once it runs past roughly 25 words.
-->

# The one decision this spec pins, stated concretely

<!-- markdownlint-disable MD036 -->

**Title**
The one decision this spec pins, stated concretely

**Lens**: SW

**Status**: planned

**Description**
The single behaviour, rule, or decision this spec fixes — what it requires and,
where there is a real choice, what it excludes.

**Rationale**
Why this decision and not the alternatives.

**Verification Description**
How a test or a review confirms the decision holds.

## Relations

**Realizes**

- [SYS-001](SYS-001-the-capability.md) — the system capability this realizes

**Related**

- [CON-001](CON-001-a-related-constraint.md) — a constraint this spec must respect
