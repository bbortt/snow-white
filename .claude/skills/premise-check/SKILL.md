---
name: premise-check
description: Use at the start of any change request that asserts a fact — before clew-draft, requirements, architect, or any code. Separates what the requester observed from what they concluded, then verifies the load-bearing factual claims against primary sources (the pinned version's docs, a real response, the code itself) before a design is built on them. Trigger whenever a request, an issue, or an existing comment asserts what a third-party system can or cannot do, what a limit or default is, why something is slow, or what this codebase already does — including when the assertion comes from the user. Do NOT trigger for requests carrying no factual premise (rename this, bump that dependency, fix this typo), or when every load-bearing premise was already verified earlier in the session.
---

# Premise Check

The governance contract (`.claude/.ai-project-context/000-agent-instructions.md`) already forbids
**you** from filling gaps with assumptions.
This skill covers the other direction: the gaps that arrive already filled, in the request.

A requester can be wrong, confidently, in good faith.
Their wrong premise reaches you pre-formatted as a fact, in an issue or a sentence, with none of
the hedging a guess would carry.
Nothing downstream re-opens it — `requirements` asks why they want it, `clew-critique` asks
whether the spec says enough, and both take the stated facts as given.
This skill is the one place the request's own claims are treated as claims.

## The case this exists for

Issue #1697 asked for Grafana Tempo as a telemetry backend.
It listed, under "The relevant Tempo endpoints are", a two-row table: search traces by attribute,
and fetch a single trace by ID.
Two endpoints the docs do both describe — presented together as the parts of one job.

The first implementation (`11d1a8fd`) used only the search endpoint.
Twenty-seven minutes later, `4cb3125b` — a commit titled _"ci(#1697): add tests"_ — added
`TRACE_BY_ID_PATH` and `fetchFullSpans`, turning one search into one search plus an HTTP round
trip **per matched trace**.
The architecture changed inside a commit that claimed to be adding tests, because writing the test
is when the search response turned out not to carry what the code expected.
At that moment the issue's second endpoint was already sitting there, pre-authorized, looking like
the intended answer.

The same commit wrote the justification into `TempoTelemetryServiceImpl`'s Javadoc, where it still
sits:

> Since downstream coverage calculators read attribute keys that can't be enumerated up front
> (arbitrary OpenAPI parameter/header names), a search only identifies matching (traceId, spanId)
> pairs; the full attribute set for each match is then fetched via the by-ID trace endpoint.

That sentence has two halves.
The first — Tempo returns only what a `select()` clause names, with no wildcard — is true.
The second is false, and it is not a claim about Tempo at all.
It is a claim about **this repo's own calculators**, and `STR-013` settled it by reading all 14 of
them: the key set is closed, and fully enumerable before the query fires.
Checking it required no external source, no running Tempo, and no permission — just reading code
that was already in the repo.

Nobody read them.
Nine later commits touched that file — a v2 API migration, a resilience pass, a clew retrace — and
not one reopened the sentence, because a comment that explains _why_ a design is necessary reads
as a decision already made rather than as an open question.
The cost was not style: the per-trace round trips ran synchronously inside the Kafka Streams poll
(`SW-008`), blew past `max.poll.interval.ms`, and fenced the consumer out of its group in
production — the incident `STR-012` now exists to tolerate.

Two failures, one lesson each:

- A **human** listed two endpoints and thereby implied an architecture.
  It was never phrased as a design decision, so it was never reviewed as one — it was just
  background, already agreed.
- An **agent**, when the code did not work, reached for the mechanism the request had already
  blessed and wrote a false claim about the project's own code to justify it.

Guard both directions.
The request is not evidence, and neither is your own account of why you did what you were told.

Note where the bad premise entered: not at design time, but at the first sign of trouble.
That is the moment to re-run this skill — when something does not work and the fix is the other
thing the requester mentioned, you are about to inherit their architecture without either of you
ever having decided on it.

## What a request is actually made of

Split every request into three layers before doing anything with it.
Requests arrive with all three fused into one sentence and nothing marking which is which;
separating them is most of this skill.

- **Goal** — what the requester wants to be true afterward.
  **Authoritative.** You do not verify a goal, you serve it.
  "Snow-White should work against an existing Tempo instance" is a goal.
- **Observation** — what they saw.
  Evidence, but partial.
  You are not checking their honesty, you are checking their _reading_ — what the log line
  actually proves, what the doc page actually says.
  "The Tempo docs list a search endpoint and a trace-by-ID endpoint" is an observation, and a
  correct one.
- **Conclusion** — the mechanism, constraint, or cause they inferred between the two.
  A hypothesis wearing a fact's clothing.
  It is the only layer that needs verifying, and it is reliably the layer stated with the most
  confidence.
  "So you search, then fetch each trace by ID" is a conclusion — and it was wrong.

The goal in #1697 was right.
The observation was right.
Only the join between them was wrong, and that is the layer nobody was looking at.

## Which premises to check — the load-bearing test

A premise is **load-bearing** if a different answer changes what you build.
Ask it plainly: _if this is false, do I build something different?_
No means do not verify it and do not mention it.
Yes means it is the entire job.

Verify in this order — the ranking is by how often the claim is wrong times what it costs:

1. **Impossibility and absence claims** — "X has no way to do Y", "the keys can't be enumerated",
   "that API doesn't support it".
   Highest priority.
   They are the most likely to be wrong, the most expensive when wrong (they foreclose the cheap
   design, so the expensive workaround gets built instead), and the least likely to be revisited —
   nobody re-checks a constraint that has already been routed around.
2. **Capability and shape claims about third-party systems** — which endpoints exist, what a
   limit or default is, what a response contains, what a flag means.
3. **Causal claims** — "it's slow because X", "that fails because Y".
   A named cause stops anyone from measuring.
4. **Claims about this codebase** — "we already do X", "nothing reads Y", "the calculators need
   arbitrary keys".
   Cheapest of all to check, and the Tempo Javadoc is why they are on the list: proximity makes
   them feel verified when they are only familiar.

## How to verify — rank your sources

1. **Run it.** A real request, a real query, a scratch test, the actual response body.
2. **Primary docs for the version actually in use.** Pin the version first — `pom.xml`, the Helm
   chart, the compose file — then read _that_ version's page.
   Tempo publishes docs per minor version and the behavior differs between them; a right answer
   from the wrong version is still a wrong answer.
3. **The dependency's own source or schema**, where the docs are silent or ambiguous.
4. **Somebody's assertion** — the request, an issue, a Javadoc, a commit message, a spec, this
   file.
   **This is not evidence.**
   It is a pointer to something nobody has checked yet.

The rule: a load-bearing premise sitting at rank 4 is promoted to rank 1–3, or the design stops
depending on it.

## Comments and issues are assertions, not evidence

Call this out separately because it is the failure that actually happened here, and because it is
the one that hides best.

A comment that exists to explain _why_ a design is necessary is load-bearing **by construction** —
justifying the design is its whole purpose.
It is also the least-audited prose in a repo: it reads as the output of a decision someone already
made carefully.

So when you touch code whose comment asserts an external constraint, either verify the constraint
or leave the comment alone.
Never carry it forward into new prose.
Restating it in a fresh spec, story, or Javadoc launders an unchecked claim into a second source
that appears to corroborate the first — which is exactly how one sentence about "keys that can't
be enumerated" outlived nine commits through the file it sat in.

## When a premise turns out to be wrong

They were mistaken, not dishonest.
Aim the correction at the claim and keep moving.

- **Don't ask permission to verify.**
  Verifying is not a decision that needs approval; it is the work.
- **Don't hand the premise back as a question.**
  "Are you sure Tempo can't return those attributes?" asks the person who was wrong to re-confirm
  their own mistake, from the same memory that produced it.
  Bring the source instead.
- **Report the delta, in this shape:** what the request assumed → what the source says, linked and
  version-pinned → what changes about the design → what is now their call.
- **A premise you could not settle is a finding, not a pass.**
  Say you could not settle it, say where you looked, and name the cheapest experiment that would —
  usually a scratch request against a real instance.
  Do not silently downgrade "unverified" to "fine".
- **If they reaffirm it after seeing the source, build it.**
  It is their system and their call.
  Record the premise and the disagreement in the artifact, so the next person inherits a decision
  rather than a fact.

## Write the verification down

Fold each verified load-bearing premise into the artifact that carries the decision — a clew
story's Problem / Context, a spec's Rationale, or the PR body for work with no spec.
Record three things: the claim, the source (a version-pinned link, or the command you ran), and
when you checked.

`STR-013` is the form to copy:

> the full inventory was confirmed by reading every calculator's attribute access in this session,
> not assumed

That clause is what turns a premise back into something re-checkable.
An unverified claim costs one sentence to write and a production incident to discover; a cited one
costs a minute to re-check when the version bumps.

## Bounding — stay cheap or get skipped

A gate that fires on everything gets routed around, and then it protects nothing.

- **Most requests have no load-bearing premises.**
  The correct output is one line — "no load-bearing premises" — and moving on.
  Do not manufacture doubt to look thorough.
- **Cap at the top few**, verified in priority order.
  If more are load-bearing than you can settle, say so rather than quietly checking the easy ones.
- **Time-box each premise.**
  Two honest attempts that do not settle it makes it a stated finding, not an expanding research
  project.
- **Never re-verify** a premise already settled this session or already cited in the artifact.
- **Never verify the goal.**
  "I want the UI in dark mode" is not a claim about the world.
  Drilling into _why they want it_ is the `requirements` skill's job, and a different question.

## Where it hooks

- **Before `clew-draft` step 1** ("Understand the work item"), for anything in clew's scope.
  This is the highest-leverage point: an unchecked premise written into a story's Problem / Context
  is inherited by every spec that story realizes, and then by the code anchored to them.
- **Before `requirements`**, for requests outside clew's scope (`api-gateway` webapp,
  `toolkit/cli`).
- **Before a fix or refactor that carries a premise** — the gap this skill closes.
  A pure fix needs no spec, so it meets no gate at all today; "make it fetch each trace by ID"
  arrives as an implementation instruction and goes straight to code.
- **When an implementation hits a wall** and the way out is another mechanism the request
  mentioned.
  `4cb3125b` is what that looks like when it goes unchecked.
- **On demand** — "check the premises on this", "is that actually true?".

How it differs from its neighbours:

- The **governance contract** forbids _you_ from assuming.
  This skill questions what the _requester_ assumed.
- **`requirements`** drills to the real need behind the request.
  It can drill through a false premise perfectly well and still land on the wrong mechanism.
- **`clew-critique`** asks whether a spec says _enough_, taking its claims as given.
  This asks whether those claims are _true_.
  Orthogonal — run both on a risk-bearing spec.

## Done when

- The request is split into goal, observation, and conclusion, and only the conclusions were
  treated as claims.
- Every load-bearing premise is verified at rank 1–3, or reported as an unsettled finding with the
  experiment that would settle it — none is left resting on somebody's assertion.
- Any premise found false is reported with its source and what it changes, before the design that
  depended on it is built.
- Each verified premise is written into the story, spec, or PR body with its source and date.
- Premises that are not load-bearing went unmentioned.
