---
name: requirements
description: Use before implementing any new feature, UI element, endpoint, or user-observable behavior change in Snow-White — i.e. whenever the user asks for a new capability rather than a fix, refactor, dependency bump, or lint/Sonar cleanup. Drills from the requested output down to the underlying non-technical need, then drafts or extends an RQ-N/NFR-N entry in docs/_pages/requirements.md and gets it confirmed before any code is written. Do NOT trigger for bug fixes restoring already-intended behavior, pure refactors, test-only changes, or changes the user has explicitly said to implement without a requirements step.
---

# Requirements-First Workflow

Snow-White's own requirements doc (`docs/_pages/requirements.md`) opens with:

> **Scope:** These requirements describe **observable behavior and outcomes** of Snow-White as a
> system.
> They are intentionally **black-box testable** and avoid internal implementation
> details.

Every non-trivial change to what the system does should trace back to one of these.
This skill
is how that gets written down — asking "why" until you reach the real need, not the requested
mechanism.

## Step 1 — Check what already exists

Read `docs/_pages/requirements.md` in full before asking anything.
The request may already be
covered by an existing `RQ-N`/`NFR-N`, or be a natural sub-point of one (e.g. `RQ-6.3
Visualization or reporting capabilities` already covers most result-display asks). If so, say so
and skip straight to implementation — don't manufacture a new requirement for something that's
already specified.

## Step 2 — Ask "why", not "what"

If the request isn't covered, don't start implementing.
The user will usually describe an
**output** ("add a button that downloads the report as JSON", "show a spinner while loading").
Your job is to find the **outcome** underneath it — the non-technical need that output happens to
serve right now, but which would still be true if the implementation were completely different.

Use `AskUserQuestion` (or plain chat questions if the framing doesn't fit multiple-choice) and
iterate 2-3 rounds of "why do you need that?" until further "why" stops revealing new
information.
Concretely:

- "Add a JUnit-download button" → _why?_ → "so people can see what failed" → _why can't they see
  it in the UI already?_ → "because they want to send it to a teammate who doesn't have access" →
  **root need: Quality-Gate evidence must be shareable with people who don't have UI access.**
- "Add a dark mode toggle" → _why?_ → "our office is bright and the white background is
  straining" → **root need: the UI must remain comfortably usable under bright ambient
  lighting** (this framing leaves room for OS-level `prefers-color-scheme` instead of a manual
  toggle, if that turns out to be the better implementation).

Stop asking once the answer stops changing what you'd build — don't interrogate for its own sake
on genuinely simple, unambiguous requests.

## Step 3 — Phrase it as an outcome, not a mechanism

The requirement must still make sense if the eventual implementation changed entirely.

- ❌ "Snow-White SHALL provide a JUnit XML download button on the result page." (describes the
  output — a specific button, a specific format, a specific page)
- ✅ "Snow-White SHALL allow Quality-Gate results to be exported in a form that can be shared
  outside the web UI." (describes the outcome — implementable as a button, an API, a CLI flag,
  whatever fits later)

No class names, component names, REST paths, or UI widget types belong in the requirement text —
those are implementation, and belong in the code/PR, not here.

## Step 4 — Draft it in the existing format, then confirm

Match the conventions already in the document exactly:

- `**RQ-N[.M]** Snow-White SHALL/SHALL NOT/MAY <outcome>.` — numbered, one sentence per bullet.
- Attach to an existing top-level `RQ-N` as a new sub-point (`RQ-N.M`) if it refines an existing
  area; only add a new top-level `RQ-N` for a genuinely new capability area.
- Add or update the `_Linked NFRs:_` line if the change has a cross-cutting quality attribute
  (performance, reliability, usability, etc. — see the `NFR-*` section at the bottom of the doc).
- Keep language as `SHALL`/`SHALL NOT`/`MAY`, matching RFC 2119-style usage already in the file.

Show the user the drafted addition (as a snippet, not yet applied) and get explicit confirmation
or edits **before** touching any implementation code.

## Step 5 — Write it down, then implement

Apply the confirmed addition to `docs/_pages/requirements.md`, keeping numbering contiguous and
the traceability notes at the bottom accurate.
Only then move on to implementation.
If the
change touches a microservice with black-box (`apptest`) coverage, consider whether the new
requirement should get an `AppTest` case too — that's Snow-White's own philosophy applied to
itself (`RQ-0.2`: black-box tests as the primary source of truth).

## When the user pushes back

If the user clearly wants to skip this ("just do it", visible impatience), don't force the full
process — ask at most one clarifying question, write a single-line requirement yourself, state it
plainly, and proceed.
Only skip entirely if they explicitly say to.
